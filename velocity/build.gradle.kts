plugins {
    id("java")
    alias(libs.plugins.shadow)
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.rapunzellib.platform.velocity)
    implementation(libs.rapunzellib.network)
    implementation(libs.rapunzellib.database.spool)

    // Velocity API
    implementation(libs.velocity.api)
    annotationProcessor(libs.velocity.api)

    // Logging
    implementation(libs.slf4j.simple)
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
        exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
    }

    assemble {
        dependsOn(shadowJar)
    }
}
