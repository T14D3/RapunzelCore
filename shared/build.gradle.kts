plugins {
    id("java")
}

group = "de.t14d3.rapunzelcore"
version = "0.0.1"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
}

dependencies {
    // Spool ORM
    implementation("de.t14d3:spool:836be7c915")
    
    // JSON serialization for cross-server communication
    implementation("com.google.code.gson:gson:2.10.1")

    // JDBC driver for MySQL (used when database.jdbc is a mysql:// URL)
    implementation("com.mysql:mysql-connector-j:9.3.0")
    
    // Logging
    implementation("org.slf4j:slf4j-api:2.0.7")

    implementation("com.github.Carleslc.Simple-YAML:Simple-Yaml:1.8.4")

    // Adventure Platform
    compileOnly("net.kyori:adventure-api:4.25.0")
    compileOnly("net.kyori:adventure-text-minimessage:4.25.0")
    compileOnly("net.kyori:adventure-text-serializer-plain:4.25.0")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}
