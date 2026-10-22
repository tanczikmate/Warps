import java.util.Properties

plugins {
    id("java-library")
    id("xyz.jpenilla.run-paper") version "3.0.2"
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

tasks {
    processResources {
        val props = mapOf("version" to version)
        filesMatching("paper-plugin.yml") {
            expand(props)
        }
    }
}

val localProperties = Properties().apply {
    val localPropertiesFile = project.rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { load(it) }
    }
}

val localPluginsDir = localProperties.getProperty("local.plugins.dir")
    ?: "${project.rootDir}/plugins"

val copyToMyServer by tasks.registering(Copy::class) {
    description = "Copy the built jar to the server folder"
    from(tasks.jar)
    into(file(localPluginsDir))
    rename { "Warps.jar" }
}

tasks.build {
    finalizedBy(copyToMyServer)
}
