package de.t14d3.rapunzelcore.database;

import de.t14d3.rapunzelcore.database.sync.DbEntitySync;
import de.t14d3.rapunzellib.database.SpoolDatabase;
import de.t14d3.rapunzellib.network.Messenger;
import de.t14d3.spool.core.EntityManager;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.Supplier;

/**
 * Compatibility facade for RapunzelCore code that expects static DB helpers.
 *
 * <p>Actual DB lifecycle is handled by RapunzelLib's {@link SpoolDatabase}.</p>
 */
public final class CoreDatabase {
    private static volatile SpoolDatabase database;
    private static volatile DbEntitySync entitySync;
    private static volatile ExecutorService executor;
    private static final Object EXECUTOR_LOCK = new Object();

    private CoreDatabase() {
    }

    public static void init(SpoolDatabase newDatabase) {
        database = newDatabase;
    }

    public static void shutdown() {
        DbEntitySync sync = entitySync;
        if (sync != null) {
            try {
                sync.close();
            } catch (Exception ignored) {
            }
        }
        entitySync = null;

        ExecutorService currentExecutor = executor;
        executor = null;
        if (currentExecutor != null) {
            try {
                currentExecutor.shutdownNow();
            } catch (Exception ignored) {
            }
        }
        database = null;
    }

    public static SpoolDatabase db() {
        SpoolDatabase current = database;
        if (current == null) {
            throw new IllegalStateException(
                "SpoolDatabase not initialized. Call CoreDatabase.init(...) during plugin enable."
            );
        }
        return current;
    }

    public static EntityManager getEntityManager() {
        return db().entityManager();
    }

    public static void startEntitySync(Messenger messenger) {
        if (messenger == null) {
            throw new IllegalArgumentException("messenger must not be null");
        }
        if (entitySync != null) {
            return;
        }
        synchronized (CoreDatabase.class) {
            if (entitySync != null) {
                return;
            }
            entitySync = new DbEntitySync(db(), messenger);
        }
    }

    public static DbEntitySync entitySync() {
        return entitySync;
    }

    public static void runLocked(Runnable runnable) {
        db().runLocked(runnable);
    }

    public static <T> T locked(Supplier<T> supplier) {
        return db().locked(supplier);
    }

    private static ExecutorService getExecutor() {
        ExecutorService current = executor;
        if (current != null && !current.isShutdown()) {
            return current;
        }
        synchronized (EXECUTOR_LOCK) {
            current = executor;
            if (current != null && !current.isShutdown()) {
                return current;
            }
            ThreadFactory factory = runnable -> {
                Thread thread = new Thread(runnable, "RapunzelCore-DB");
                thread.setDaemon(true);
                return thread;
            };
            executor = Executors.newSingleThreadExecutor(factory);
            return executor;
        }
    }

    public static CompletableFuture<Void> runAsync(Runnable runnable) {
        if (runnable == null) return CompletableFuture.completedFuture(null);
        return CompletableFuture.runAsync(runnable, getExecutor());
    }

    public static <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier) {
        if (supplier == null) return CompletableFuture.completedFuture(null);
        return CompletableFuture.supplyAsync(supplier, getExecutor());
    }

    public static CompletableFuture<Void> runLockedAsync(Runnable runnable) {
        if (runnable == null) return CompletableFuture.completedFuture(null);
        return runAsync(() -> runLocked(runnable));
    }

    public static <T> CompletableFuture<T> lockedAsync(Supplier<T> supplier) {
        if (supplier == null) return CompletableFuture.completedFuture(null);
        return supplyAsync(() -> locked(supplier));
    }

    public static void flushAsync() {
        db().flushAsync();
    }
}
