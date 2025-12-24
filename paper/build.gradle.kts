plugins {
    id("java")
    alias(libs.plugins.paperweight.userdev)
    alias(libs.plugins.shadow)
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.rapunzellib.platform.paper)
    implementation(libs.rapunzellib.network)
    implementation(libs.rapunzellib.database.spool)

    paperweight.paperDevBundle(libs.versions.paper.dev.bundle.get())

    // CommandAPI
    implementation(libs.commandapi.paper.shade)

    // Reflections for module discovery
    implementation(libs.reflections)

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
        exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
    }

    assemble {
        dependsOn(shadowJar)
    }
}
