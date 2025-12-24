import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("base")
    id("java")
    id("maven-publish")
    alias(libs.plugins.shadow)
    alias(libs.plugins.run.paper)
    alias(libs.plugins.rapunzellib)
}

allprojects {
    group = "de.t14d3.rapunzelcore"
    version = "0.0.1"
}

// Needed because this build script directly references subproject tasks/source sets when assembling combined plugin jars.
evaluationDependsOn(":shared")
evaluationDependsOn(":paper")
evaluationDependsOn(":velocity")

tasks {
    rapunzellib.messagesFile.set(file("shared/src/main/resources/messages.yml"))
    rapunzellib.messageKeyCallOwners.add("de.t14d3.rapunzelcore.MessageHandler")
    rapunzellib.messageKeyCallOwners.add("de.t14d3.rapunzelcore.modules.teleports.TpaCommands")
    rapunzellib.messageKeyCallMethods.set(setOf("getMessage", "getRaw", "notifyOnServer"))

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
        // Avoid signed dependency metadata breaking classloading (Velocity is strict about this).
        exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
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
        // Avoid signed dependency metadata breaking classloading (Velocity is strict about this).
        exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
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
        // Avoid signed dependency metadata breaking classloading (Velocity is strict about this).
        exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
    }



    named("assemble") {
        dependsOn(paperJar, velocityJar, allJar)
    }


    named<ShadowJar>("shadowJar") {
        enabled = false
        dependsOn(paperJar, velocityJar, allJar)
    }

    // Validate message usage across all compiled modules (shared + platform).
    named<de.t14d3.rapunzellib.gradle.tasks.ValidateMessagesTask>("rapunzellibValidateMessages") {
        dependsOn(
            project(":shared").tasks.named("classes"),
            project(":paper").tasks.named("classes"),
            project(":velocity").tasks.named("classes"),
        )

        classesDirs.from(
            project(":shared").the<SourceSetContainer>()["main"].output.classesDirs,
            project(":paper").the<SourceSetContainer>()["main"].output.classesDirs,
            project(":velocity").the<SourceSetContainer>()["main"].output.classesDirs,
        )
    }

    runServer {
        dependsOn(allJar)
        minecraftVersion(libs.versions.minecraft.get())
        pluginJars.from(allJar.flatMap { it.archiveFile })
    }

    withType<de.t14d3.rapunzellib.gradle.tasks.RunServersTask>().configureEach {

        // Paper: accept EULA and set basic server.properties per instance.
        replace(
            "eula.txt",
            """(?s)\A.*\z""",
            "eula=true\n"
        )
        replace(
            "server.properties",
            """(?s)\A.*\z""",
            """
                server-port={{paper_port}}
                online-mode=false
                enable-rcon=false
                enable-query=false
                enforce-secure-profile=false
                motd=RapunzelCore {{server_name}}

            """.trimIndent()
        )

        // Paper: configure Velocity forwarding (safe even if Velocity is disabled; Paper will ignore when secret is blank).
        replace(
            "config/paper-global.yml",
            """(?s)\A.*\z""",
            """
                proxies:
                  velocity:
                    enabled: {{velocity_enabled}}
                    online-mode: true
                    secret: "{{velocity_secret}}"

            """.trimIndent()
        )

        // Velocity: ensure secret file is populated (runner provides a stable {{velocity_secret}} per baseDir).
        replace(
            "forwarding.secret",
            """(?s)\A.*\z""",
            "{{velocity_secret}}\n"
        )

        // Velocity: patch bind, forwarding mode, and backend servers.
        replace(
            "velocity.toml",
            """(?m)^\s*bind\s*=\s*".*"\s*$""",
            """bind = "0.0.0.0:{{velocity_port}}""""
        )
        replace(
            "velocity.toml",
            """(?m)^\s*player-info-forwarding-mode\s*=\s*".*"\s*$""",
            """player-info-forwarding-mode = "MODERN""""
        )
        replace(
            "velocity.toml",
            """(?ms)^\[servers\]\R.*?(?=^\[|\z)""",
            "{{velocity_servers_block}}\n"
        )
        replace(
            "velocity.toml",
            """(?ms)^\[forced-hosts\]\R.*?(?=^\[|\z)""",
            "{{velocity_forced_hosts_block}}\n"
        )

        // If MySQL is enabled (docker), use the runner-provided JDBC string in plugin configs.
        // Configure the database name via `-PmultiMysqlDatabase=rapunzelcore` (default overridden here).
        mysqlDatabase.set(providers.gradleProperty("multiMysqlDatabase").orElse("rapunzelcore"))

        if (mysqlEnabled.get()) {
            listOf(
                // Paper data folder
                "plugins/RapunzelCore/config.yml",
                // Velocity data folder (plugin id: rapunzelcore)
                "plugins/rapunzelcore/config.yml",
            ).forEach { path ->
                replace(
                    path,
                    """  jdbc: .*""",
                    """  jdbc: "{{mysql_jdbc}}""""
                )
            }
        }
    }

    register("runMultiServers") {
        group = "run"
        description = "Alias for rapunzellibRunServers."
        dependsOn("rapunzellibRunServers")
    }

    register("runPerfServers") {
        group = "run"
        description = "Alias for rapunzellibRunPerfServers."

        dependsOn("rapunzellibRunPerfServers")
    }

    // Avoid Velocity plugin load failures when shaded dependencies contain signature files.
    listOf(paperJar, velocityJar, allJar).forEach { jarTask ->
        jarTask.configure {
            exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
        }
    }
}

publishing {
    publications {
        create<org.gradle.api.publish.maven.MavenPublication>("rapunzelCore") {
            artifactId = "rapunzelcore"
            artifact(tasks.named<ShadowJar>("paperJar"))
            artifact(tasks.named<ShadowJar>("velocityJar"))
            artifact(tasks.named<ShadowJar>("allJar"))
        }
    }
}

tasks.register("publishLocal") {
    group = "publishing"
    description = "Builds and publishes RapunzelCore jars to Maven Local."
    dependsOn("publishToMavenLocal")
}
