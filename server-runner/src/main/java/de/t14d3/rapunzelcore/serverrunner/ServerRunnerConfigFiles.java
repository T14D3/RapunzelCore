package de.t14d3.rapunzelcore.serverrunner;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

final class ServerRunnerConfigFiles {
    enum Platform { PAPER, VELOCITY }

    private static final String DEFAULT_RAPUNZEL_CONFIG_YML = """
        # Module configuration
        # Set to false to disable specific modules
        modules:
          script: true
          joinleave: true
          interaction: true
          shulker: true
          warps: false
          homes: false
          teamchat: true
          commands: true
          teleports: true
          chat: true

        # Database configuration
        database:
          # JDBC connection string
          jdbc: "jdbc:sqlite:plugins/RapunzelCore/rapunzelcore.db"
        """;

    private static final SecureRandom RNG = new SecureRandom();
    private static final String SECRET_ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private ServerRunnerConfigFiles() {
    }

    static void writeEula(Path serverDir) throws IOException {
        Files.writeString(serverDir.resolve("eula.txt"), "eula=true\n", StandardCharsets.UTF_8);
    }

    static void writePaperProperties(Path serverDir, int port) throws IOException {
        String content = ""
            + "server-port=" + port + "\n"
            + "online-mode=false\n"
            + "enable-rcon=false\n"
            + "enable-query=false\n"
            + "enforce-secure-profile=false\n"
            + "motd=RapunzelCore Test Server\n";
        Files.writeString(serverDir.resolve("server.properties"), content, StandardCharsets.UTF_8);
    }

    static void writeRapunzelJdbcConfig(Path serverDir, Platform platform, Path pluginJar, String jdbc) throws IOException {
        Path dataDir = switch (platform) {
            case PAPER -> serverDir.resolve("plugins").resolve("RapunzelCore");
            case VELOCITY -> serverDir.resolve("plugins").resolve("rapunzelcore");
        };
        Files.createDirectories(dataDir);

        String baseConfig = readConfigFromPluginJar(pluginJar);
        String updated = patchJdbcInYaml(baseConfig, jdbc);
        Files.writeString(dataDir.resolve("config.yml"), updated, StandardCharsets.UTF_8);
    }

    static void bootstrapVelocityConfig(Path serverDir, String javaBin, List<String> jvmArgs) throws IOException, InterruptedException {
        Path configFile = serverDir.resolve("velocity.toml");
        if (Files.exists(configFile)) return;

        List<String> cmd = ServerProcess.javaCommand(javaBin, jvmArgs, "velocity.jar", List.of());
        ServerProcess p = ServerProcess.start("velocity-bootstrap", serverDir, cmd, null);

        long deadline = System.currentTimeMillis() + 30_000L;
        while (System.currentTimeMillis() < deadline) {
            if (Files.exists(configFile)) break;
            if (!p.isAlive()) break;
            Thread.sleep(250);
        }

        p.destroy();
        p.waitFor();

        if (!Files.exists(configFile)) {
            throw new IOException("Velocity did not generate velocity.toml within timeout");
        }
    }

    static String ensureVelocityForwardingSecret(Path serverDir) throws IOException {
        Path secretFile = serverDir.resolve("forwarding.secret");
        if (Files.exists(secretFile)) {
            String existing = Files.readString(secretFile, StandardCharsets.UTF_8).trim();
            if (!existing.isBlank()) return existing;
        }

        String generated = randomSecret(16);
        Files.writeString(secretFile, generated + "\n", StandardCharsets.UTF_8);
        return generated;
    }

    static void patchVelocityConfig(Path serverDir, int port, List<Integer> backendPorts) throws IOException {
        Path configFile = serverDir.resolve("velocity.toml");

        Path backupFile = configFile.resolveSibling(configFile.getFileName() + ".backup");
        Files.copy(configFile, backupFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        String original = Files.readString(configFile, StandardCharsets.UTF_8);
        String updated = original;

        updated = updated.replace("player-info-forwarding-mode = \"none\"", "player-info-forwarding-mode = \"modern\"");

        updated = replaceServersBlock(updated, backendPorts);
        updated = replaceForcedHostsBlock(updated, "localhost", List.of("lobby"));

        if (!updated.equals(original)) {
            Files.writeString(configFile, updated, StandardCharsets.UTF_8);
        }
    }

    static void patchPaperGlobalConfigForVelocity(
        Path paperServerDir,
        String javaBin,
        List<String> jvmArgs,
        String velocityForwardingSecret
    ) throws IOException, InterruptedException {
        Path configFile = paperServerDir.resolve("config").resolve("paper-global.yml");
        if (!Files.exists(configFile)) {
            bootstrapPaperGlobalConfig(paperServerDir, javaBin, jvmArgs, configFile);
        }

        String original = Files.readString(configFile, StandardCharsets.UTF_8);
        String updated = patchPaperGlobalYamlForVelocity(original, velocityForwardingSecret);
        if (!updated.equals(original)) {
            Path backupFile = configFile.resolveSibling(configFile.getFileName() + ".backup");
            Files.copy(configFile, backupFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            Files.writeString(configFile, updated, StandardCharsets.UTF_8);
        }
    }

    private static void bootstrapPaperGlobalConfig(
        Path paperServerDir,
        String javaBin,
        List<String> jvmArgs,
        Path configFile
    ) throws IOException, InterruptedException {
        Files.createDirectories(configFile.getParent());

        List<String> cmd = ServerProcess.javaCommand(javaBin, jvmArgs, "paper.jar", List.of("--nogui"));
        ServerProcess p = ServerProcess.start("paper-bootstrap", paperServerDir, cmd, null);

        long deadline = System.currentTimeMillis() + 60_000L;
        while (System.currentTimeMillis() < deadline) {
            if (Files.exists(configFile)) break;
            if (!p.isAlive()) break;
            Thread.sleep(250);
        }

        p.destroy();
        p.waitFor();

        if (!Files.exists(configFile)) {
            throw new IOException("Paper did not generate config/paper-global.yml within timeout");
        }
    }

    private static String readConfigFromPluginJar(Path pluginJar) {
        if (pluginJar == null) return DEFAULT_RAPUNZEL_CONFIG_YML;
        if (!Files.isRegularFile(pluginJar)) return DEFAULT_RAPUNZEL_CONFIG_YML;

        try (ZipFile zip = new ZipFile(pluginJar.toFile(), StandardCharsets.UTF_8)) {
            ZipEntry entry = zip.getEntry("config.yml");
            if (entry == null) return DEFAULT_RAPUNZEL_CONFIG_YML;
            try (var in = zip.getInputStream(entry)) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (Exception ignored) {
            return DEFAULT_RAPUNZEL_CONFIG_YML;
        }
    }

    private static String patchJdbcInYaml(String yaml, String jdbc) {
        String replacement = "  jdbc: \"" + jdbc.replace("\"", "\\\"") + "\"";
        var pattern = java.util.regex.Pattern.compile("(?m)^\\s*jdbc\\s*:\\s*.*$");
        var m = pattern.matcher(yaml);
        if (m.find()) return m.replaceFirst(java.util.regex.Matcher.quoteReplacement(replacement));
        String sep = yaml.endsWith("\n") ? "" : "\n";
        return yaml + sep + "\ndatabase:\n" + replacement + "\n";
    }

    private static String replaceServersBlock(String toml, List<Integer> backendPorts) {
        String serversBlock = buildServersBlock(backendPorts);
        var pattern = java.util.regex.Pattern.compile("(?s)^\\[servers\\]\\R.*?(?=^\\[|\\z)", java.util.regex.Pattern.MULTILINE);
        var m = pattern.matcher(toml);
        if (m.find()) {
            return m.replaceFirst(java.util.regex.Matcher.quoteReplacement(serversBlock));
        }
        String sep = toml.endsWith("\n") ? "" : "\n";
        return toml + sep + "\n" + serversBlock;
    }

    private static String replaceForcedHostsBlock(String toml, String host, List<String> servers) {
        String forcedHostsBlock = buildForcedHostsBlock(host, servers);
        var pattern = java.util.regex.Pattern.compile("(?s)^\\[forced-hosts\\]\\R.*?(?=^\\[|\\z)", java.util.regex.Pattern.MULTILINE);
        var m = pattern.matcher(toml);
        if (m.find()) {
            return m.replaceFirst(java.util.regex.Matcher.quoteReplacement(forcedHostsBlock));
        }
        String sep = toml.endsWith("\n") ? "" : "\n";
        return toml + sep + "\n" + forcedHostsBlock;
    }

    private static String buildForcedHostsBlock(String host, List<String> servers) {
        StringBuilder sb = new StringBuilder();
        sb.append("[forced-hosts]\n");
        sb.append("\"").append(host).append("\" = [\"").append(String.join("\", \"", servers)).append("\"]\n");
        return sb.toString();
    }

    private static String buildServersBlock(List<Integer> backendPorts) {
        StringBuilder sb = new StringBuilder();
        sb.append("[servers]\n");
        for (int i = 0; i < backendPorts.size(); i++) {
            int backendPort = backendPorts.get(i);
            String name = (i == 0) ? "lobby" : ("backend" + (i + 1));
            sb.append(name).append(" = \"127.0.0.1:").append(backendPort).append("\"\n");
        }
        return sb.toString();
    }

    private static String patchPaperGlobalYamlForVelocity(String yaml, String secret) {
        String newline = yaml.contains("\r\n") ? "\r\n" : "\n";
        String escapedSecret = secret.replace("'", "''");

        List<String> lines = Arrays.asList(yaml.split("\\R", -1));
        var out = new java.util.ArrayList<String>(lines.size() + 8);

        boolean proxiesSeen = false;
        boolean inProxies = false;
        int proxiesIndent = -1;

        boolean velocitySeen = false;
        boolean inVelocity = false;
        int velocityIndent = -1;

        boolean enabledUpdated = false;
        boolean secretUpdated = false;

        for (String line : lines) {
            String trimmed = stripLeadingSpaces(line);
            String key = yamlKey(trimmed);
            int indent = leadingSpaces(line);

            if (key != null) {
                if (inVelocity && indent <= velocityIndent) {
                    if (!enabledUpdated) out.add(spaces(velocityIndent + 2) + "enabled: true");
                    if (!secretUpdated) out.add(spaces(velocityIndent + 2) + "secret: '" + escapedSecret + "'");
                    inVelocity = false;
                }

                if (inProxies && indent <= proxiesIndent) {
                    if (!velocitySeen) {
                        out.add(spaces(proxiesIndent + 2) + "velocity:");
                        out.add(spaces(proxiesIndent + 4) + "enabled: true");
                        out.add(spaces(proxiesIndent + 4) + "secret: '" + escapedSecret + "'");
                        velocitySeen = true;
                        enabledUpdated = true;
                        secretUpdated = true;
                    }
                    inProxies = false;
                }

                if (key.equals("proxies") && indent == 0) {
                    proxiesSeen = true;
                    inProxies = true;
                    proxiesIndent = indent;
                } else if (inProxies && key.equals("velocity") && indent > proxiesIndent) {
                    velocitySeen = true;
                    inVelocity = true;
                    velocityIndent = indent;
                } else if (inVelocity && key.equals("enabled") && indent > velocityIndent) {
                    line = spaces(indent) + "enabled: true";
                    enabledUpdated = true;
                } else if (inVelocity && key.equals("secret") && indent > velocityIndent) {
                    line = spaces(indent) + "secret: '" + escapedSecret + "'";
                    secretUpdated = true;
                }
            }

            out.add(line);
        }

        if (inVelocity) {
            if (!enabledUpdated) out.add(spaces(velocityIndent + 2) + "enabled: true");
            if (!secretUpdated) out.add(spaces(velocityIndent + 2) + "secret: '" + escapedSecret + "'");
            inVelocity = false;
        }

        if (inProxies && !velocitySeen) {
            out.add(spaces(proxiesIndent + 2) + "velocity:");
            out.add(spaces(proxiesIndent + 4) + "enabled: true");
            out.add(spaces(proxiesIndent + 4) + "secret: '" + escapedSecret + "'");
            velocitySeen = true;
            enabledUpdated = true;
            secretUpdated = true;
            inProxies = false;
        }

        if (!proxiesSeen) {
            out.add("proxies:");
            out.add("  velocity:");
            out.add("    enabled: true");
            out.add("    secret: '" + escapedSecret + "'");
        }

        return String.join(newline, out);
    }

    private static int leadingSpaces(String s) {
        int i = 0;
        while (i < s.length() && s.charAt(i) == ' ') i++;
        return i;
    }

    private static String stripLeadingSpaces(String s) {
        int i = 0;
        while (i < s.length() && s.charAt(i) == ' ') i++;
        return s.substring(i);
    }

    private static String yamlKey(String trimmedLine) {
        if (trimmedLine.isEmpty()) return null;
        if (trimmedLine.startsWith("#")) return null;
        if (trimmedLine.startsWith("-")) return null;
        int colon = trimmedLine.indexOf(':');
        if (colon <= 0) return null;
        String key = trimmedLine.substring(0, colon).trim();
        if (key.isEmpty()) return null;
        if (!key.matches("[A-Za-z0-9_-]+")) return null;
        return key;
    }

    private static String spaces(int count) {
        if (count <= 0) return "";
        return " ".repeat(count);
    }

    private static String randomSecret(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(SECRET_ALPHABET.charAt(RNG.nextInt(SECRET_ALPHABET.length())));
        }
        return sb.toString();
    }
}

