import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("base")
    id("java")
    id("com.gradleup.shadow") version "9.0.0-beta8"
    id("xyz.jpenilla.run-paper") version "2.0.1"
}

group = "de.t14d3.rapunzelcore"
version = "0.0.1"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")

    maven("https://jitpack.io")
}



tasks {


    val buildDir = file(layout.buildDirectory.get().asFile.resolve("libs"))

    val paperJar by registering(ShadowJar::class) {
        archiveBaseName.set("RapunzelCore")
        archiveVersion.set(project.version.toString())
        archiveClassifier.set("paper")
        destinationDirectory.set(buildDir)

        manifest {
            attributes["paperweight-mappings-namespace"] = "mojang"
        }

        dependsOn(
            project(":shared").tasks.named("classes"),
            project(":paper").tasks.named("classes"),
        )

        from(project(":shared").the<SourceSetContainer>()["main"].output)
        from(project(":paper").the<SourceSetContainer>()["main"].output)

        configurations = listOf(
            project(":shared").configurations.getByName("runtimeClasspath"),
            project(":paper").configurations.getByName("runtimeClasspath"),
        )

        mergeServiceFiles()

        relocate("org.reflections", "de.t14d3.rapunzelcore.libs.reflections")
        relocate("redis.clients.jedis", "de.t14d3.rapunzelcore.libs.jedis")
    }

    val velocityJar by registering(ShadowJar::class) {
        archiveBaseName.set("RapunzelCore")
        archiveVersion.set(project.version.toString())
        archiveClassifier.set("velocity")
        destinationDirectory.set(buildDir)

        dependsOn(
            project(":shared").tasks.named("classes"),
            project(":velocity").tasks.named("classes"),
        )

        from(project(":shared").the<SourceSetContainer>()["main"].output)
        from(project(":velocity").the<SourceSetContainer>()["main"].output)

        configurations = listOf(
            project(":shared").configurations.getByName("runtimeClasspath"),
            project(":velocity").configurations.getByName("runtimeClasspath"),
        )

        mergeServiceFiles()
        relocate("redis.clients.jedis", "de.t14d3.rapunzelcore.libs.jedis")
    }

    val allJar by registering(ShadowJar::class) {
        archiveBaseName.set("RapunzelCore")
        archiveVersion.set(project.version.toString())
        archiveClassifier.set("all")
        destinationDirectory.set(buildDir)

        manifest {
            attributes["paperweight-mappings-namespace"] = "mojang"
        }

        dependsOn(
            project(":shared").tasks.named("classes"),
            project(":paper").tasks.named("classes"),
            project(":velocity").tasks.named("classes"),
        )

        from(project(":shared").the<SourceSetContainer>()["main"].output)
        from(project(":paper").the<SourceSetContainer>()["main"].output)
        from(project(":velocity").the<SourceSetContainer>()["main"].output)

        configurations = listOf(
            project(":shared").configurations.getByName("runtimeClasspath"),
            project(":paper").configurations.getByName("runtimeClasspath"),
            project(":velocity").configurations.getByName("runtimeClasspath"),
        )

        mergeServiceFiles()

        relocate("org.reflections", "de.t14d3.rapunzelcore.libs.reflections")
        relocate("redis.clients.jedis", "de.t14d3.rapunzelcore.libs.jedis")
    }



    named("assemble") {
        dependsOn(paperJar, velocityJar, allJar)
    }


    named<ShadowJar>("shadowJar") {
        enabled = false
        dependsOn(paperJar, velocityJar, allJar)
    }

    runServer {
        dependsOn(allJar)
        minecraftVersion("1.21.10")
        pluginJars.from(allJar.flatMap { it.archiveFile })
    }

    /**
     * Builds the Paper/Velocity plugin jars, then runs the `server-runner` CLI which downloads
     * temporary Paper + Velocity servers via Fill v3 and copies the plugin jars into their
     * respective `plugins/` folders.
     *
     * Configure via Gradle properties:
     * - `-PmultiPaperVersion=1.21.10` (default: 1.21.10)
     * - `-PmultiPaperCount=2` (default: 2)
     * - `-PmultiPaperBasePort=25565` (default: 25565)
     * - `-PmultiVelocityVersion=<version on Fill>` (required)
     * - `-PmultiVelocityPort=25577` (default: 25577)
     * - `-PmultiRunnerJvmArgs=-Xmx2G,-Dfoo=bar` (optional, comma-separated)
     */
    register<JavaExec>("runMultiServers") {
        group = "run"
        description = "Runs Velocity + multiple Paper backends via Fill v3 (uses server-runner)."

        dependsOn(paperJar, velocityJar, project(":server-runner").tasks.named("classes"))

        val runner = project(":server-runner")
        val mainSourceSet = runner.the<SourceSetContainer>()["main"]
        classpath = mainSourceSet.runtimeClasspath
        mainClass.set("de.t14d3.rapunzelcore.serverrunner.ServerRunnerMain")

        doFirst {
            val paperVersion = (findProperty("multiPaperVersion") as String?) ?: "1.21.10"
            val paperCount = (findProperty("multiPaperCount") as String?) ?: "2"
            val paperBasePort = (findProperty("multiPaperBasePort") as String?) ?: "25566"
            val velocityVersion = (findProperty("multiVelocityVersion") as String?) ?: "latest"
            val velocityPort = (findProperty("multiVelocityPort") as String?) ?: "25565"
            val runnerJvmArgsRaw = findProperty("multiRunnerJvmArgs") as String?

            val paperPluginJar = paperJar.flatMap { it.archiveFile }.get().asFile.absolutePath
            val velocityPluginJar = velocityJar.flatMap { it.archiveFile }.get().asFile.absolutePath

            if (!runnerJvmArgsRaw.isNullOrBlank()) {
                jvmArgs(runnerJvmArgsRaw.split(',').map { it.trim() }.filter { it.isNotEmpty() })
            }

            args(
                "--paper-version", paperVersion,
                "--paper-count", paperCount,
                "--paper-base-port", paperBasePort,
                "--paper-plugin", paperPluginJar,
                "--velocity-version", velocityVersion,
                "--velocity-port", velocityPort,
                "--velocity-plugin", velocityPluginJar,
                "--mysql",
            )
        }
    }
}
