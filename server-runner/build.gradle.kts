plugins {
    id("application")
    id("java")
}

group = "de.t14d3.rapunzelcore"
version = "0.0.1"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    implementation("com.google.code.gson:gson:2.10.1")
}

application {
    mainClass.set("de.t14d3.rapunzelcore.serverrunner.ServerRunnerMain")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

