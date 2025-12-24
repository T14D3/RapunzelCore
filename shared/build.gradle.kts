plugins {
    id("java")
    alias(libs.plugins.rapunzellib)
}

dependencies {
    implementation(libs.spool)

    implementation(libs.rapunzellib.api)
    implementation(libs.rapunzellib.network)
    implementation(libs.rapunzellib.database.spool)

    // JSON serialization for cross-server communication
    implementation(libs.gson)

    // Module discovery (used by shared ReflectionsUtil)
    implementation(libs.reflections)

    // JDBC driver for MySQL (used when database.jdbc is a mysql:// URL)        
    implementation(libs.mysql.driver)
    
    // Logging
    implementation(libs.slf4j.api)

    // Adventure Platform
    compileOnly(libs.adventure.api)
    compileOnly(libs.adventure.text.minimessage)
    compileOnly(libs.adventure.text.serializer.plain)
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

// Messages are shared across subprojects; validate once from the root project (aggregated task).
tasks.named("rapunzellibValidateMessages") {
    enabled = false
}
