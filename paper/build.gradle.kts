plugins {
    id("java")
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.19"
    id("com.gradleup.shadow") version "9.0.0-beta8"
}

group = "de.t14d3.rapunzelcore"
version = "0.0.1"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
}

dependencies {
    implementation(project(":shared"))


    implementation("de.t14d3:spool:836be7c915")

    paperweight.paperDevBundle("1.21.10-R0.1-SNAPSHOT")

    // CommandAPI
    implementation("dev.jorel:commandapi-paper-shade:11.1.0")

    // Reflections for module discovery
    implementation("org.reflections:reflections:0.10.2")

    implementation("com.github.Carleslc.Simple-YAML:Simple-Yaml:1.8.4")

}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"

}

tasks {
    shadowJar {
        archiveBaseName.set("RapunzelCore")
        archiveClassifier.set("")

        // Relocate dependencies to avoid conflicts
        relocate("org.reflections", "de.t14d3.rapunzelcore.libs.reflections")
    }

    assemble {
        dependsOn(shadowJar)
    }
}

