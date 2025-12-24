package de.t14d3.rapunzelcore.configsync;

import de.t14d3.rapunzelcore.RapunzelCore;
import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.config.YamlConfig;
import de.t14d3.rapunzellib.network.Messenger;
import de.t14d3.rapunzellib.network.filesync.FileSyncEndpoint;
import de.t14d3.rapunzellib.network.filesync.FileSyncResult;
import de.t14d3.rapunzellib.network.filesync.FileSyncRole;
import de.t14d3.rapunzellib.network.filesync.FileSyncSpec;
import de.t14d3.rapunzellib.scheduler.ScheduledTask;
import de.t14d3.rapunzellib.scheduler.Scheduler;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.time.Duration;
import java.util.Locale;
import java.util.Objects;

public final class CoreConfigSync {
    private static final String LOCAL_CONFIG_FILE = "config-sync.local.yml";
    private static final String GROUP_ID = "rapunzelcore";

    private static final Object lock = new Object();
    private static volatile boolean enabled;
    private static volatile FileSyncEndpoint endpoint;
    private static volatile ScheduledTask repeatingTask;
    private static volatile Thread watchThread;
    private static volatile WatchService watchService;
    private static volatile ScheduledTask watchDebounceTask;

    private CoreConfigSync() {
    }

    public static void bootstrap(RapunzelCore core) {
        Objects.requireNonNull(core, "core");

        synchronized (lock) {
            shutdown();

            var logger = Rapunzel.context().logger();
            Path dataDir = Rapunzel.context().dataDirectory();
            YamlConfig local = Rapunzel.context().configs().load(
                dataDir.resolve(LOCAL_CONFIG_FILE),
                LOCAL_CONFIG_FILE
            );

            if (!local.getBoolean("configSync.enabled", false)) {
                enabled = false;
                return;
            }

            FileSyncRole role = parseRole(local.getString("configSync.role", "FOLLOWER"));
            String authorityServerName = local.getString("configSync.authorityServerName", "").trim();
            boolean autoRequestOnInvalidate = local.getBoolean("configSync.autoRequestOnInvalidate", true);
            boolean deleteExtraneous = local.getBoolean("configSync.deleteExtraneous", false);
            boolean syncOnStartup = local.getBoolean("configSync.syncOnStartup", true);
            boolean autoReload = local.getBoolean("configSync.apply.autoReload", false);
            Duration reloadDelay = Duration.ofMillis(local.getLong("configSync.apply.reloadDelayMs", 250));

            if (role == FileSyncRole.FOLLOWER && authorityServerName.isBlank()) {
                logger.warn("[ConfigSync] Enabled but configSync.authorityServerName is blank; disabling.");
                enabled = false;
                return;
            }

            Path root = dataDir.resolve(local.getString("configSync.root", ".")).normalize();
            if (!root.isAbsolute()) {
                root = dataDir.resolve(root).normalize();
            }

            FileSyncSpec.Builder specBuilder = FileSyncSpec.builder(root)
                .deleteExtraneous(deleteExtraneous);

            var includes = local.getStringList("configSync.includes");
            var excludes = local.getStringList("configSync.excludes");

            if (includes.isEmpty()) {
                specBuilder
                    .includeGlob("config.yml")
                    .includeGlob("modules/**")
                    .includeGlob("messages.yml");
            } else {
                for (String glob : includes) {
                    specBuilder.includeGlob(glob);
                }
            }

            if (excludes.isEmpty()) {
                specBuilder.excludeGlob(LOCAL_CONFIG_FILE);
            } else {
                for (String glob : excludes) {
                    specBuilder.excludeGlob(glob);
                }
            }

            FileSyncSpec spec = specBuilder.build();

            Messenger messenger = core.getMessenger();
            Scheduler scheduler = Rapunzel.context().scheduler();

            endpoint = new FileSyncEndpoint(
                messenger,
                scheduler,
                logger,
                GROUP_ID,
                spec,
                role,
                authorityServerName,
                autoRequestOnInvalidate,
                Duration.ofSeconds(local.getLong("configSync.timeouts.requestSeconds", 5)),
                Duration.ofSeconds(local.getLong("configSync.timeouts.transferSeconds", 20)),
                (int) local.getLong("configSync.transfer.maxChunkBytes", 8192),
                local.getLong("configSync.transfer.maxPayloadBytes", 5L * 1024L * 1024L),
                new FileSyncEndpoint.Listener() {
                    @Override
                    public void onApplied(FileSyncResult result) {
                        logger.info("[ConfigSync] Applied update (wrote {}, deleted {})", result.filesWritten(), result.filesDeleted());
                        if (!autoReload) return;
                        scheduler.runLater(reloadDelay, () -> scheduler.run(core::reloadPlugin));
                    }

                    @Override
                    public void onError(String message, Throwable error) {
                        logger.warn("[ConfigSync] {}", message);
                    }
                },
                null
            );

            enabled = true;

            if (syncOnStartup && role == FileSyncRole.FOLLOWER) {
                scheduler.runLater(Duration.ofSeconds(1), () -> requestSync().exceptionally(err -> null));
            }

            boolean scheduleEnabled = local.getBoolean("configSync.schedule.enabled", false);
            long scheduleSeconds = local.getLong("configSync.schedule.periodSeconds", 60);
            if (scheduleEnabled && scheduleSeconds > 0) {
                String mode = local.getString("configSync.schedule.mode", "").trim().toLowerCase(Locale.ROOT);
                boolean doBroadcast = mode.isBlank()
                    ? (role == FileSyncRole.AUTHORITY)
                    : mode.equals("broadcast");
                if (doBroadcast && role == FileSyncRole.AUTHORITY) {
                    repeatingTask = scheduler.runRepeating(Duration.ofSeconds(2), Duration.ofSeconds(scheduleSeconds), CoreConfigSync::broadcastInvalidate);
                } else if (!doBroadcast && role == FileSyncRole.FOLLOWER) {
                    repeatingTask = scheduler.runRepeating(Duration.ofSeconds(2), Duration.ofSeconds(scheduleSeconds), () -> requestSync().exceptionally(err -> null));
                }
            }

            boolean watchEnabled = local.getBoolean("configSync.watch.enabled", false);
            long debounceMs = local.getLong("configSync.watch.debounceMs", 1000);
            if (watchEnabled && role == FileSyncRole.AUTHORITY) {
                startWatcher(spec, scheduler, logger, Duration.ofMillis(Math.max(0, debounceMs)));
            }
        }
    }

    public static boolean isEnabled() {
        return enabled && endpoint != null;
    }

    public static FileSyncRole role() {
        FileSyncEndpoint ep = endpoint;
        return (ep == null) ? null : ep.role();
    }

    public static void broadcastInvalidate() {
        FileSyncEndpoint ep = endpoint;
        if (ep == null) throw new IllegalStateException("Config sync not enabled");
        ep.broadcastInvalidate();
    }

    public static java.util.concurrent.CompletableFuture<FileSyncResult> requestSync() {
        FileSyncEndpoint ep = endpoint;
        if (ep == null) return java.util.concurrent.CompletableFuture.failedFuture(new IllegalStateException("Config sync not enabled"));
        return ep.requestSync();
    }

    public static void shutdown() {
        ScheduledTask task = repeatingTask;
        repeatingTask = null;
        if (task != null) {
            try {
                task.cancel();
            } catch (Exception ignored) {
            }
        }

        ScheduledTask debounce = watchDebounceTask;
        watchDebounceTask = null;
        if (debounce != null) {
            try {
                debounce.cancel();
            } catch (Exception ignored) {
            }
        }

        WatchService ws = watchService;
        watchService = null;
        if (ws != null) {
            try {
                ws.close();
            } catch (Exception ignored) {
            }
        }

        Thread thread = watchThread;
        watchThread = null;
        if (thread != null) {
            try {
                thread.interrupt();
            } catch (Exception ignored) {
            }
        }

        FileSyncEndpoint ep = endpoint;
        endpoint = null;
        enabled = false;
        if (ep != null) {
            try {
                ep.close();
            } catch (Exception ignored) {
            }
        }
    }

    private static FileSyncRole parseRole(String raw) {
        if (raw == null) return FileSyncRole.FOLLOWER;
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        try {
            return FileSyncRole.valueOf(normalized);
        } catch (Exception ignored) {
            return FileSyncRole.FOLLOWER;
        }
    }

    private static void startWatcher(FileSyncSpec spec, Scheduler scheduler, org.slf4j.Logger logger, Duration debounce) {
        if (watchThread != null) return;

        try {
            watchService = spec.rootDirectory().getFileSystem().newWatchService();
            // Watch root + its subdirectories (best-effort, non-recursive API).
            registerAllDirs(spec.rootDirectory(), watchService);
        } catch (Exception e) {
            logger.warn("[ConfigSync] Failed to start file watcher: {}", e.getMessage());
            return;
        }

        watchThread = new Thread(() -> runWatchLoop(spec, scheduler, logger, debounce), "rapunzelcore-configsync-watch");
        watchThread.setDaemon(true);
        watchThread.start();
    }

    private static void runWatchLoop(FileSyncSpec spec, Scheduler scheduler, org.slf4j.Logger logger, Duration debounce) {
        WatchService ws = watchService;
        if (ws == null) return;

        while (!Thread.currentThread().isInterrupted()) {
            WatchKey key;
            try {
                key = ws.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                break;
            }

            Path dir = (Path) key.watchable();
            boolean matched = false;
            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();
                if (kind == StandardWatchEventKinds.OVERFLOW) continue;

                Object ctx = event.context();
                if (!(ctx instanceof Path relName)) continue;

                Path child = dir.resolve(relName).normalize();
                Path relativeToRoot;
                try {
                    relativeToRoot = spec.rootDirectory().relativize(child);
                } catch (Exception ignored) {
                    continue;
                }

                // Track newly created directories.
                if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                    try {
                        if (java.nio.file.Files.isDirectory(child)) {
                            registerAllDirs(child, ws);
                        }
                    } catch (Exception ignored) {
                    }
                }

                if (spec.matches(relativeToRoot)) {
                    matched = true;
                }
            }

            try {
                key.reset();
            } catch (Exception ignored) {
            }

            if (!matched) continue;

            if (watchDebounceTask != null) continue;
            watchDebounceTask = scheduler.runLater(debounce, () -> {
                try {
                    watchDebounceTask = null;
                    if (isEnabled() && role() == FileSyncRole.AUTHORITY) {
                        broadcastInvalidate();
                        logger.info("[ConfigSync] Detected file change; broadcasted invalidate.");
                    }
                } catch (Exception e) {
                    logger.warn("[ConfigSync] Failed to broadcast invalidate: {}", e.getMessage());
                }
            });
        }
    }

    private static void registerAllDirs(Path root, WatchService ws) throws IOException {
        if (root == null || ws == null) return;
        if (!java.nio.file.Files.exists(root)) return;
        try (var stream = java.nio.file.Files.walk(root)) {
            for (Path p : stream.filter(java.nio.file.Files::isDirectory).toList()) {
                try {
                    p.register(
                        ws,
                        StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_MODIFY,
                        StandardWatchEventKinds.ENTRY_DELETE
                    );
                } catch (Exception ignored) {
                }
            }
        }
    }
}
