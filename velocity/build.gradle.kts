plugins {
    id("java")
    id("com.gradleup.shadow") version "9.0.0-beta8"
}

group = "de.t14d3.rapunzelcore"
version = "0.0.1"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://maven.velocitypowered.com/snapshot/")
    maven("https://jitpack.io")
}

dependencies {
    implementation(project(":shared"))
    
    // Velocity API
    implementation("com.velocitypowered:velocity-api:3.0.0-SNAPSHOT")
    annotationProcessor("com.velocitypowered:velocity-api:3.0.0-SNAPSHOT")
    
    // Redis for database communication (optional)
    implementation("redis.clients:jedis:5.1.0")
    
    // Logging
    implementation("org.slf4j:slf4j-simple:2.0.7")
    implementation("com.github.Carleslc.Simple-YAML:Simple-Yaml:1.8.4")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks {
    shadowJar {
        archiveBaseName.set("RapunzelCore-Velocity")
        archiveClassifier.set("")
        
        // Relocate dependencies to avoid conflicts
        relocate("com.google.gson", "de.t14d3.rapunzelcore.libs.gson")
        relocate("redis.clients.jedis", "de.t14d3.rapunzelcore.libs.jedis")
    }
    
    assemble {
        dependsOn(shadowJar)
    }
}
