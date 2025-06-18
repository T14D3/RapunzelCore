plugins {
    id("idea")
    id("java")
    id("xyz.jpenilla.run-paper") version "2.3.1"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("io.ebean") version "14.1.0"
}

registerCustomOutputTask("Saturn", "C:\\dev\\Minecraft\\Server\\mcss_win-x86-64_v13.8.0\\servers\\Item Instinct\\plugins")

group = "de.bydennyy"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven {
        name = "sonatype"
        url = uri("https://oss.sonatype.org/content/groups/public/")
    }
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {

    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")

    //implementation 'net.coreprotect:coreprotect:22.4)'

    compileOnly("org.projectlombok:lombok:1.18.36")
    annotationProcessor("org.projectlombok:lombok:1.18.36")

    //Luckperms für Ränge und Status
    compileOnly("net.luckperms:api:5.4")

}

tasks {
    runServer {
        downloadPlugins {
            url("https://download.luckperms.net/1584/bukkit/loader/LuckPerms-Bukkit-5.5.0.jar")
            url("https://cdn.modrinth.com/data/3wmN97b8/versions/eXnRWJUj/multiverse-core-4.3.16.jar")
            url("https://ci.athion.net/job/FastAsyncWorldEdit/1113/artifact/artifacts/FastAsyncWorldEdit-Bukkit-2.13.1-SNAPSHOT-1113.jar")

        }
        minecraftVersion("1.21.4")
    }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks.named<Jar>("jar") {
    archiveBaseName.set("byDennyysEssentials")
    archiveClassifier.set("")
    archiveVersion.set("")
}

tasks.register<Copy>("pluginBuild") {
    group = "bydennyy"
    from(layout.buildDirectory.file( "libs/byDennyysEssentials.jar"))
    into(layout.buildDirectory)
    dependsOn("jar")
}

fun registerCustomOutputTask(name: String, path: String) {
    if (!System.getProperty("os.name").lowercase().contains("windows")) {
        return
    }

    tasks.register<Copy>("build$name") {
        group = "development"
        doNotTrackState("because")
        dependsOn("pluginBuild")

        from(layout.buildDirectory.file("byDennyysEssentials.jar"))
        into(file(path))

        // Optional rename logic, if you want to include it:
        //rename { fileName: String ->
        //    fileName.replace("byDennyysEssentials-$version.jar", "byDennyysEssentials.jar")
        //}
    }
}