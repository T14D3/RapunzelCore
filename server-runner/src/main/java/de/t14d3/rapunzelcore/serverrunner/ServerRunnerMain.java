package de.t14d3.rapunzelcore.serverrunner;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class ServerRunnerMain {
    private static final String DEFAULT_JAVA = "java";

    public static void main(String[] args) throws Exception {
        Config cfg = Config.parse(args);

        Path baseDir = cfg.baseDir != null ? cfg.baseDir : Path.of("run", "server-runner");
        Path cacheDir = cfg.cacheDir != null ? cfg.cacheDir : baseDir.resolve("cache");
        Path instancesDir = cfg.instancesDir != null ? cfg.instancesDir : baseDir.resolve("instances");

        Files.createDirectories(cacheDir);
        Files.createDirectories(instancesDir);

        FillV3Client fill = new FillV3Client();

        List<ServerProcess> processes = new ArrayList<>();
        final String[] mysqlContainerNameForShutdown = new String[] { null };
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            for (ServerProcess p : processes) {
                if (p.isAlive()) p.destroy();
            }
            for (ServerProcess p : processes) {
                if (p.isAlive()) p.destroyForcibly();
            }
            if (mysqlContainerNameForShutdown[0] != null) {
                cleanupMysqlContainer(baseDir, mysqlContainerNameForShutdown[0]);
            }
        }));

        String mysqlJdbc = null;
        if (cfg.mysqlEnabled) {
            String containerName = cfg.mysqlContainerName != null
                ? cfg.mysqlContainerName
                : "rapunzelcore-mysql";
            mysqlContainerNameForShutdown[0] = containerName;
            ensureMysqlContainerRunning(cfg, baseDir, containerName);
            waitForPortOpen("127.0.0.1", cfg.mysqlPort, 60_000L);
            mysqlJdbc = cfg.mysqlJdbc();
        }

        Path velocityJar = null;
        if (cfg.velocityVersion != null) {
            FillV3Client.ResolvedBuild build = fill.resolveLatestBuild("velocity", cfg.velocityVersion);
            velocityJar = fill.downloadJar("velocity", cfg.velocityVersion, build, cacheDir);
        }

        Path paperJar = null;
        if (cfg.paperVersion != null) {
            FillV3Client.ResolvedBuild build = fill.resolveLatestBuild("paper", cfg.paperVersion);
            paperJar = fill.downloadJar("paper", cfg.paperVersion, build, cacheDir);
        }

        if (cfg.paperCount > 0 && paperJar == null) throw new IllegalStateException("Paper jar resolution failed");
        if (cfg.velocityVersion != null && velocityJar == null) {
            throw new IllegalStateException("Velocity jar resolution failed");
        }

        String velocityForwardingSecret = null;
        if (cfg.velocityVersion != null) {
            Path dir = createInstanceDir(instancesDir, "velocity");
            Files.copy(velocityJar, dir.resolve("velocity.jar"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            if (cfg.velocityPlugin != null) installPlugin(dir, cfg.velocityPlugin);
            if (mysqlJdbc != null) {
                ServerRunnerConfigFiles.writeRapunzelJdbcConfig(
                    dir,
                    ServerRunnerConfigFiles.Platform.VELOCITY,
                    cfg.velocityPlugin,
                    mysqlJdbc
                );
            }
            ServerRunnerConfigFiles.bootstrapVelocityConfig(dir, cfg.javaBin, cfg.jvmArgs);
            ServerRunnerConfigFiles.patchVelocityConfig(
                dir,
                cfg.velocityPort,
                cfg.velocityBackendPorts(cfg.paperBasePort, cfg.paperCount)
            );
            velocityForwardingSecret = ServerRunnerConfigFiles.ensureVelocityForwardingSecret(dir);

            List<String> cmd = ServerProcess.javaCommand(cfg.javaBin, cfg.jvmArgs, "velocity.jar", List.of());
            processes.add(ServerProcess.start("velocity", dir, cmd, null));
        }

        for (int i = 0; i < cfg.paperCount; i++) {
            int port = cfg.paperBasePort + i;
            String name = "paper-" + (i + 1);
            Path dir = createInstanceDir(instancesDir, name);
            Files.copy(paperJar, dir.resolve("paper.jar"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            ServerRunnerConfigFiles.writeEula(dir);
            ServerRunnerConfigFiles.writePaperProperties(dir, port);
            if (velocityForwardingSecret != null) {
                ServerRunnerConfigFiles.patchPaperGlobalConfigForVelocity(
                    dir,
                    cfg.javaBin,
                    cfg.jvmArgs,
                    velocityForwardingSecret
                );
            }
            if (cfg.paperPlugin != null) installPlugin(dir, cfg.paperPlugin);
            if (mysqlJdbc != null) {
                ServerRunnerConfigFiles.writeRapunzelJdbcConfig(
                    dir,
                    ServerRunnerConfigFiles.Platform.PAPER,
                    cfg.paperPlugin,
                    mysqlJdbc
                );
            }

            List<String> programArgs = new ArrayList<>();
            programArgs.add("--nogui");
            List<String> cmd = ServerProcess.javaCommand(cfg.javaBin, cfg.jvmArgs, "paper.jar", programArgs);
            processes.add(ServerProcess.start(name, dir, cmd, null));
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (processes.isEmpty()) {
            System.err.println("Nothing to run. Provide --paper-version/--paper-count and/or --velocity-version.");
            System.exit(2);
            return;
        }

        List<CompletableFuture<Process>> exits = processes.stream().map(ServerProcess::onExit).toList();
        CompletableFuture.anyOf(exits.toArray(new CompletableFuture[0])).join();

        int exitCode = 0;
        for (int i = 0; i < processes.size(); i++) {
            ServerProcess p = processes.get(i);
            if (!p.isAlive()) {
                exitCode = exits.get(i).get().exitValue();
                break;
            }
        }

        for (ServerProcess p : processes) {
            if (p.isAlive()) p.destroy();
        }
        for (ServerProcess p : processes) {
            if (p.isAlive()) p.destroyForcibly();
        }

        for (ServerProcess p : processes) {
            p.waitFor();
        }
        System.exit(exitCode);
    }

    private static Path createInstanceDir(Path instancesDir, String name) throws IOException {
        Path dir = instancesDir.resolve(name );
        Files.createDirectories(dir);
        Files.createDirectories(dir.resolve("plugins"));
        return dir;
    }

    private static void installPlugin(Path serverDir, Path pluginJar) throws IOException {
        Path pluginsDir = serverDir.resolve("plugins");
        Files.createDirectories(pluginsDir);
        Files.copy(pluginJar, pluginsDir.resolve(pluginJar.getFileName()), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    private static List<String> dockerMysqlCommand(Config cfg, String containerName) {
        // We run detached and clean up explicitly in the shutdown hook so a
        // crashed run doesn't leave the next run broken due to "container already exists".
        return Arrays.asList(
            "docker", "run", "-d",
            "--name", containerName,
            "-p", cfg.mysqlPort + ":3306",
            "-e", "MYSQL_ROOT_PASSWORD=" + cfg.mysqlRootPassword,
            "-e", "MYSQL_DATABASE=" + cfg.mysqlDatabase,
            cfg.mysqlImage
        );
    }

    private static void ensureMysqlContainerRunning(Config cfg, Path workingDir, String containerName)
        throws IOException, InterruptedException {
        DockerContainerState state = dockerContainerState(workingDir, containerName);
        if (state == DockerContainerState.RUNNING) return;

        if (state == DockerContainerState.EXISTS_STOPPED) {
            DockerCommandResult start = runCommand(workingDir, List.of("docker", "start", containerName));
            if (start.exitCode != 0) {
                throw new IOException("Failed to start existing MySQL container '" + containerName + "': " + start.output);
            }
            return;
        }

        DockerCommandResult run = runCommand(workingDir, dockerMysqlCommand(cfg, containerName));
        if (run.exitCode != 0) {
            throw new IOException("Failed to start MySQL container '" + containerName + "': " + run.output);
        }
    }

    private static void cleanupMysqlContainer(Path workingDir, String containerName) {
        try {
            // Best-effort: ignore errors if it doesn't exist anymore.
            runCommand(workingDir, List.of("docker", "rm", "-f", containerName));
        } catch (Exception ignored) {
        }
    }

    private enum DockerContainerState {
        NOT_FOUND,
        EXISTS_STOPPED,
        RUNNING
    }

    private static DockerContainerState dockerContainerState(Path workingDir, String containerName)
        throws IOException, InterruptedException {
        DockerCommandResult res = runCommand(workingDir, List.of(
            "docker", "inspect", "-f", "{{.State.Running}}", containerName
        ));
        if (res.exitCode != 0) {
            // docker prints "No such object" on stderr/stdout depending on platform.
            if (res.output.toLowerCase().contains("no such object")) return DockerContainerState.NOT_FOUND;
            return DockerContainerState.NOT_FOUND;
        }
        String v = res.output.trim();
        if (v.equalsIgnoreCase("true")) return DockerContainerState.RUNNING;
        return DockerContainerState.EXISTS_STOPPED;
    }

    private record DockerCommandResult(int exitCode, String output) {}

    private static DockerCommandResult runCommand(Path workingDir, List<String> command)
        throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(workingDir.toFile());
        pb.redirectErrorStream(true);
        Process p = pb.start();
        String out;
        try (var in = p.getInputStream()) {
            out = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        int code = p.waitFor();
        return new DockerCommandResult(code, out);
    }

    private static void waitForPortOpen(String host, int port, long timeoutMs) throws IOException, InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        IOException last = null;
        while (System.currentTimeMillis() < deadline) {
            try (Socket s = new Socket()) {
                s.connect(new InetSocketAddress(host, port), 750);
                return;
            } catch (IOException e) {
                last = e;
                Thread.sleep(500);
            }
        }
        throw new IOException("Timed out waiting for " + host + ":" + port + " to accept connections", last);
    }

    private static final class Config {
        final String javaBin;
        final List<String> jvmArgs;

        final String paperVersion;
        final int paperCount;
        final int paperBasePort;
        final Path paperPlugin;

        final String velocityVersion;
        final int velocityPort;
        final Path velocityPlugin;

        final Path baseDir;
        final Path cacheDir;
        final Path instancesDir;

        final boolean mysqlEnabled;
        final int mysqlPort;
        final String mysqlDatabase;
        final String mysqlRootPassword;
        final String mysqlImage;
        final String mysqlContainerName;

        private Config(
            String javaBin,
            List<String> jvmArgs,
            String paperVersion,
            int paperCount,
            int paperBasePort,
            Path paperPlugin,
            String velocityVersion,
            int velocityPort,
            Path velocityPlugin,
            Path baseDir,
            Path cacheDir,
            Path instancesDir,
            boolean mysqlEnabled,
            int mysqlPort,
            String mysqlDatabase,
            String mysqlRootPassword,
            String mysqlImage,
            String mysqlContainerName
        ) {
            this.javaBin = javaBin;
            this.jvmArgs = jvmArgs;
            this.paperVersion = paperVersion;
            this.paperCount = paperCount;
            this.paperBasePort = paperBasePort;
            this.paperPlugin = paperPlugin;
            this.velocityVersion = velocityVersion;
            this.velocityPort = velocityPort;
            this.velocityPlugin = velocityPlugin;
            this.baseDir = baseDir;
            this.cacheDir = cacheDir;
            this.instancesDir = instancesDir;
            this.mysqlEnabled = mysqlEnabled;
            this.mysqlPort = mysqlPort;
            this.mysqlDatabase = mysqlDatabase;
            this.mysqlRootPassword = mysqlRootPassword;
            this.mysqlImage = mysqlImage;
            this.mysqlContainerName = mysqlContainerName;
        }

        static Config parse(String[] args) {
            Map<String, String> flags = new HashMap<>();
            List<String> jvmArgs = new ArrayList<>();

            for (int i = 0; i < args.length; i++) {
                String a = args[i];
                if (a.equals("--help") || a.equals("-h")) usageAndExit(0);
                if (a.startsWith("--jvm-arg=")) {
                    jvmArgs.add(a.substring("--jvm-arg=".length()));
                    continue;
                }
                if (a.equals("--jvm-arg")) {
                    if (i + 1 >= args.length) usageAndExit(2);
                    jvmArgs.add(args[++i]);
                    continue;
                }

                if (!a.startsWith("--")) usageAndExit(2);
                String key = a.substring(2);
                if (key.equals("mysql")) {
                    flags.put("mysql", "true");
                    continue;
                }
                String value;
                int eq = key.indexOf('=');
                if (eq >= 0) {
                    value = key.substring(eq + 1);
                    key = key.substring(0, eq);
                } else {
                    if (i + 1 >= args.length) usageAndExit(2);
                    value = args[++i];
                }
                flags.put(key, value);
            }

            String javaBin = flags.getOrDefault("java", DEFAULT_JAVA);
            String paperVersion = flags.getOrDefault("paper-version", "latest");
            int paperCount = parseInt(flags.getOrDefault("paper-count", "0"), "paper-count");
            int paperBasePort = parseInt(flags.getOrDefault("paper-base-port", "25566"), "paper-base-port");
            Path paperPlugin = flags.containsKey("paper-plugin") ? Path.of(flags.get("paper-plugin")) : null;

            String velocityVersion = flags.get("velocity-version");
            if (velocityVersion != null && velocityVersion.isBlank()) velocityVersion = null;
            if (velocityVersion != null && velocityVersion.equalsIgnoreCase("latest")) velocityVersion = "latest";
            int velocityPort = parseInt(flags.getOrDefault("velocity-port", "25565"), "velocity-port");
            Path velocityPlugin = flags.containsKey("velocity-plugin") ? Path.of(flags.get("velocity-plugin")) : null;

            Path baseDir = flags.containsKey("base-dir") ? Path.of(flags.get("base-dir")) : null;
            Path cacheDir = flags.containsKey("cache-dir") ? Path.of(flags.get("cache-dir")) : null;
            Path instancesDir = flags.containsKey("instances-dir") ? Path.of(flags.get("instances-dir")) : null;

            boolean mysqlEnabled = Boolean.parseBoolean(flags.getOrDefault("mysql", "false"));
            int mysqlPort = parseInt(flags.getOrDefault("mysql-port", "3307"), "mysql-port");
            String mysqlDatabase = flags.getOrDefault("mysql-database", "db");
            String mysqlRootPassword = flags.getOrDefault("mysql-root-password", "root");
            String mysqlImage = flags.getOrDefault("mysql-image", "mysql:latest");
            String mysqlContainerName = flags.get("mysql-container-name");

            return new Config(
                javaBin,
                jvmArgs,
                paperVersion,
                paperCount,
                paperBasePort,
                paperPlugin,
                velocityVersion,
                velocityPort,
                velocityPlugin,
                baseDir,
                cacheDir,
                instancesDir,
                mysqlEnabled,
                mysqlPort,
                mysqlDatabase,
                mysqlRootPassword,
                mysqlImage,
                mysqlContainerName
            );
        }

        String mysqlJdbc() {
            // Use query params so we don't rely on external config, and keep it simple for local dev.
            String user = "root";
            String password = urlEncode(mysqlRootPassword);
            String db = urlEncode(mysqlDatabase);
            return "jdbc:mysql://127.0.0.1:" + mysqlPort + "/" + db
                + "?user=" + user
                + "&password=" + password
                + "&useSSL=false"
                + "&allowPublicKeyRetrieval=true";
        }

        List<Integer> velocityBackendPorts(int basePort, int count) {
            List<Integer> ports = new ArrayList<>();
            for (int i = 0; i < count; i++) ports.add(basePort + i);
            return ports;
        }

        private static String urlEncode(String s) {
            try {
                return java.net.URLEncoder.encode(s, StandardCharsets.UTF_8);
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid value for URL encoding");
            }
        }

        private static int parseInt(String value, String name) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid --" + name + ": " + value);
            }
        }

        private static void usageAndExit(int code) {
            System.out.println("""
                RapunzelCore server-runner (Fill v3)

                Downloads (via fill.papermc.io v3) and starts temporary Paper + Velocity instances in parallel.

                Required:
                  --paper-count <n>              e.g. 2 (use 0 to skip Paper)
                Optional:
                  --paper-version <mcVersion>    e.g. 1.21.10 or 'latest' (default: latest)
                  --velocity-version <version>   e.g. 3.4.0-SNAPSHOT or 'latest' (omit to skip Velocity, default: latest if provided)
                  --paper-base-port <port>       default 25566
                  --velocity-port <port>         default 25565
                  --paper-plugin <pathToJar>     copied into each Paper plugins/
                  --velocity-plugin <pathToJar>  copied into Velocity plugins/
                  --java <javaBin>               default 'java'
                  --jvm-arg <arg>                repeatable (e.g. --jvm-arg -Xmx2G)
                  --base-dir <dir>               default run/server-runner
                  --cache-dir <dir>              default <base-dir>/cache
                  --instances-dir <dir>          default <base-dir>/instances

                MySQL (Docker, optional):
                  --mysql                        start a local MySQL container (requires docker)
                  --mysql-port <port>            host port to bind (default 3307)
                  --mysql-database <name>        default rapunzelcore
                  --mysql-root-password <pw>     default root
                  --mysql-image <image:tag>      default mysql:8.4
                  --mysql-container-name <name>  default rapunzelcore-mysql-<timestamp>
                """);
            System.exit(code);
        }
    }
}
