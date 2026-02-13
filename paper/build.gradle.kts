plugins {
    id("java")
    alias(libs.plugins.paperweight.userdev)
    alias(libs.plugins.shadow)
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.rapunzellib.platform.paper)
    implementation(libs.rapunzellib.network)
    implementation(libs.rapunzellib.common)
    implementation(libs.rapunzellib.database.spool)
    implementation(libs.rapunzellib.nbt)
    implementation(libs.rapunzellib.nbt.paper)
 implementation(libs.rapunzellib.events)

    paperweight.paperDevBundle(libs.versions.paper.dev.bundle.get())

    // CommandAPI
    implementation(libs.commandapi.paper.shade)

    compileOnly(libs.vault) {
        exclude(group = "org.bukkit", module = "bukkit")
    }

    // Reflections for module discovery
    implementation(libs.reflections)

}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.add("--enable-preview")

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
