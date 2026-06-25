plugins {
    kotlin("jvm") version "2.3.21"
    id("org.openjfx.javafxplugin") version "0.1.0"
    application
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("io.github.oshai:kotlin-logging-jvm:5.1.0")
    implementation("io.github.jason-lang:jason-interpreter:3.2.0")
    val javaFxVersion = "21.0.2"
    implementation("org.openjfx:javafx-controls:$javaFxVersion")
    implementation("org.openjfx:javafx-graphics:$javaFxVersion")
    implementation("org.openjfx:javafx-base:$javaFxVersion")
}

javafx {
    version = "21.0.2"
    modules("javafx.controls", "javafx.graphics", "javafx.base")
}

application {
    mainClass.set("View.GuiAppKt")
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