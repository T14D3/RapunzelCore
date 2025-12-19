import java.util.Properties

plugins {
    id("java")
    id("xyz.jpenilla.run-paper") version "2.3.1"
    id("com.gradleup.shadow") version "9.0.0-beta8"
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.19"
}

group = "de.t14d3"
version = "0.0.1"

repositories {
    mavenCentral()
    maven {
        name = "papermc-repo"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    maven {
        name = "sonatype"
        url = uri("https://oss.sonatype.org/content/groups/public/")
    }
    maven {
        name = "Central Portal Snapshots"
        url = uri("https://central.sonatype.com/repository/maven-snapshots/")
        // Only search this repository for the specific dependency
        content {
            includeModule("dev.jorel", "commandapi")
        }
    }
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    paperweight.paperDevBundle("1.21.10-R0.1-SNAPSHOT")
    implementation("dev.jorel:commandapi-paper-shade:11.0.1-SNAPSHOT")
    implementation("org.reflections:reflections:0.9.11")
    implementation("de.t14d3:spool:b3b13b2bcf")
}

tasks {
    runServer {
        minecraftVersion("1.21.10")
    }
}

val targetJavaVersion = 21
java {
    val javaVersion = JavaVersion.toVersion(targetJavaVersion)
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
    if (JavaVersion.current() < javaVersion) {
        toolchain.languageVersion = JavaLanguageVersion.of(targetJavaVersion)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"

    if (targetJavaVersion >= 10 || JavaVersion.current().isJava10Compatible) {
        options.release.set(targetJavaVersion)
    }
    dependsOn("checkMessageKeys")
}

tasks.shadowJar {
    manifest {
        attributes["paperweight-mappings-namespace"] = "mojang"
    }
}

tasks.register("checkMessageKeys") {
    group = "verification"
    description = "Checks if all message keys used in the code are present in messages.properties"

    doLast {
        val sourceDir = project.layout.projectDirectory.dir("src/main/java")
        val messagesFile = project.layout.projectDirectory.file("src/main/resources/messages.properties")

        // Load message keys from properties file
        val definedKeys = mutableSetOf<String>()
        if (messagesFile.asFile.exists()) {
            val props = Properties()
            messagesFile.asFile.inputStream().use { props.load(it) }
            definedKeys.addAll(props.keys.map { it.toString() })
        } else {
            throw GradleException("messages.properties file not found at ${messagesFile.asFile.absolutePath}")
        }

        // Find all Java files
        val javaFiles = project.fileTree(sourceDir) {
            include("**/*.java")
        }

        // Extract used message keys.
        // This regex captures literal strings passed to getMessage(...). It will capture both:
        //   getMessage("some.key")
        //   getMessage("prefix." + something)
        // We treat captured literals that end with '.' as dynamic prefixes.
        val usedKeys = mutableSetOf<String>()
        val regex = Regex("""getMessage\s*\(\s*"([^"]+)"(?:\s*\+\s*[^)]*)?\)""")

        javaFiles.forEach { file ->
            val content = file.readText()
            regex.findAll(content).forEach { match ->
                usedKeys.add(match.groupValues[1])
            }
        }

        // Determine missing keys.
        // If a used key ends with '.', assume it's a dynamic prefix and accept it if
        // at least one defined key starts with that prefix.
        val missingKeys = usedKeys.filter { usedKey ->
            if (usedKey.endsWith(".")) {
                definedKeys.none { it.startsWith(usedKey) }
            } else {
                !definedKeys.contains(usedKey)
            }
        }.toSortedSet()

        if (missingKeys.isNotEmpty()) {
            val errorMessage = buildString {
                appendLine("The following message keys are used in the code but not defined in messages.properties:")
                missingKeys.forEach { key ->
                    appendLine("  - $key")
                }
                appendLine("Please add the missing keys to src/main/resources/messages.properties")
            }
            throw GradleException(errorMessage)
        } else {
            println("All message keys used in the code are present in messages.properties")
        }
    }
}
