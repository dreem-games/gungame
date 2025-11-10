import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.plugins.JavaPluginExtension
import java.io.File

plugins {
    // Root‑level plugin declarations; sub‑projects reuse these IDs
    id("com.github.johnrengelman.shadow") version "8.1.1" apply false
    id("io.freefair.lombok") version "9.1.0" apply false
}

// ─────────────────────────────────────────────────────────────────────────────
// Global constants (stable versions from Maven Central)
// ─────────────────────────────────────────────────────────────────────────────
extra.apply {
    set("appName", "gungame")
    set("gdxVersion", "1.13.5")
    set("gdxControllersVersion", "2.2.4")
    set("box2dlightsVersion", "1.5")
}

repositories {
    gradlePluginPortal()
}

subprojects {
    plugins.withId("java") {
        extensions.configure<JavaPluginExtension> {
            toolchain.languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    // Each sub‑project needs repos when they are built in isolation
    repositories {
        mavenLocal()
        mavenCentral()
    }
}

// ===========================================================================
// Core (shared) module
// ===========================================================================
project(":core") {
    apply(plugin = "java-library")
    apply(plugin = "io.freefair.lombok")

    val gdxVersion: String by rootProject.extra
    val gdxControllersVersion: String by rootProject.extra
    val box2dlightsVersion: String by rootProject.extra

    dependencies {
        // libGDX core + freetype + box2d (pure Java)
        add("api", "com.badlogicgames.gdx:gdx:$gdxVersion")
        add("api", "com.badlogicgames.gdx:gdx-freetype:$gdxVersion")
        add("api", "com.badlogicgames.gdx:gdx-box2d:$gdxVersion")
        add("api", "com.badlogicgames.box2dlights:box2dlights:$box2dlightsVersion")

        // Game‑pad abstraction layer (Java‑only)
        add("implementation", "com.badlogicgames.gdx-controllers:gdx-controllers-core:$gdxControllersVersion")
    }
}

// ===========================================================================
// Desktop launcher module
// ===========================================================================
project(":desktop") {
    apply(plugin = "java-library")
    apply(plugin = "com.github.johnrengelman.shadow")

    val gdxVersion: String by rootProject.extra
    val gdxControllersVersion: String by rootProject.extra
    val appName: String by rootProject.extra

    dependencies {
        add("implementation", project(":core"))

        // LWJGL3 backend & code jar
        add("implementation", "com.badlogicgames.gdx:gdx-backend-lwjgl3:$gdxVersion")

        // Native libs for core, freetype, box2d & controllers
        add("runtimeOnly", "com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-desktop")
        add("runtimeOnly", "com.badlogicgames.gdx:gdx-freetype-platform:$gdxVersion:natives-desktop")
        add("runtimeOnly", "com.badlogicgames.gdx:gdx-box2d-platform:$gdxVersion:natives-desktop")

        // Controllers code jar (contains natives inside)
        add("implementation", "com.badlogicgames.gdx-controllers:gdx-controllers-desktop:$gdxControllersVersion")
    }

    tasks.named<ShadowJar>("shadowJar") {
        // Produce gungame.jar right in the project root
        archiveFileName.set("$appName.jar")
        destinationDirectory.set(rootProject.layout.projectDirectory)

        manifest.attributes["Main-Class"] = "com.gungame.DesktopLauncher"
        mergeServiceFiles()

        // Embed all native jars into the fat‑JAR
        val runtimeCp = project.configurations.getByName("runtimeClasspath")
        runtimeCp.resolve()
            .filter { it.name.contains("natives") || it.name.contains("desktop") }
            .toSet()
            .forEach { jar: File -> from(zipTree(jar)) }
    }
}
