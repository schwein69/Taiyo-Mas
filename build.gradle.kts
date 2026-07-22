plugins {
    kotlin("jvm") version "2.4.10"
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.10"
    id("org.jetbrains.compose") version "1.12.0-beta02"
    application
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    google()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("io.github.oshai:kotlin-logging-jvm:5.1.0")
    implementation("io.github.jason-lang:jason-interpreter:3.2.0")
    implementation(compose.desktop.currentOs)
    implementation("org.jetbrains.compose.material:material:1.12.0-beta02")
    implementation("org.jetbrains.compose.ui:ui:1.12.0-beta02")
    implementation("org.jetbrains.compose.foundation:foundation:1.12.0-beta02")
}


application {
    mainClass.set("view.GuiAppKt")
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}

val mainSourceSet = sourceSets.getByName("main")

file(projectDir).listFiles()?.filter { it.extension == "mas2j" }?.forEach { mas2jFile ->
    val taskName = "run${mas2jFile.nameWithoutExtension.replaceFirstChar { it.uppercase() }}Mas"

    tasks.register<JavaExec>(taskName) {
        group = "application"
        description = "Avvia il sistema multi-agente definito in ${mas2jFile.name}"
        classpath = mainSourceSet.runtimeClasspath
        mainClass.set("jason.infra.centralised.RunCentralisedMAS")
        args(mas2jFile.absolutePath)
        standardInput = System.`in`
        javaLauncher.set(javaToolchains.launcherFor(java.toolchain))
    }
}