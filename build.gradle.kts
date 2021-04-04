import io.github.nucleuspowered.gradle.enums.getLevel
import io.github.nucleuspowered.gradle.task.RelNotesTask
import io.github.nucleuspowered.gradle.task.StdOutExec
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.ByteArrayOutputStream
import java.net.URL
import java.nio.charset.StandardCharsets
import java.nio.file.Files

val kotlin_version: String by extra
buildscript {
    var kotlin_version: String by extra
    kotlin_version = "1.3.61"
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath(kotlin("gradle-plugin", kotlin_version))
    }
}

plugins {
    java
    idea
    eclipse
    `maven-publish`
    // id("com.github.hierynomus.license") version "0.15.0" apply false
    id("org.cadixdev.licenser") version "0.5.1" apply false
    id("net.kyori.blossom") version "1.2.0"
    id("com.github.johnrengelman.shadow") version "5.2.0"
    id("org.spongepowered.gradle.plugin") version "1.0.2"
    // id("org.spongepowered.gradle.plugin") version "0.11.0-SNAPSHOT"
    kotlin("jvm") version "1.3.61"
}
apply {
    plugin("kotlin")
}

// Until I can figure out how to get Blossom to accept task outputs, if at all
// this will have to do.
fun getGitCommit() : String {
    return try {
        val byteOut = ByteArrayOutputStream()
        project.exec {
            commandLine = "git rev-parse --short HEAD".split(" ")
            standardOutput = byteOut
        }
        byteOut.toString("UTF-8").trim()
    } catch (ex: Exception) {
        // ignore
        "unknown"
    }
}

allprojects {

    apply(plugin = "org.cadixdev.licenser")

    configure<org.cadixdev.gradle.licenser.LicenseExtension> {
        header = rootProject.file("HEADER.txt")
        newLine = false
        exclude("**/*.info")
        exclude("assets/**")
        exclude("*.kts")
        exclude("**/*.json")
        exclude("*.properties")
        exclude("*.txt")
    }

}


extra["gitHash"] = getGitCommit()

// Get the Level
val nucVersion = project.properties["nucleusVersion"]?.toString()!!
val nucSuffix : String? = project.properties["nucleusVersionSuffix"]?.toString()

var level = getLevel(nucVersion, nucSuffix)
val spongeVersion: String = project.properties["declaredApiVersion"]!!.toString()
val versionString: String = if (nucSuffix == null) {
    nucVersion
} else {
    "$nucVersion-$nucSuffix"
}
val filenameSuffix = "SpongeAPI$spongeVersion"
version = versionString

project(":nucleus-api").version = versionString
project(":nucleus-core").version = versionString

group = "io.github.nucleuspowered"

defaultTasks.add("licenseFormat")
defaultTasks.add("build")

sponge {
    apiVersion("${project.properties["spongeApiVersion"]}")
    platform(org.spongepowered.gradle.common.SpongePlatform.VANILLA)
    plugin("nucleus") {
        loader(org.spongepowered.gradle.plugin.config.PluginLoaders.JAVA_PLAIN)
        displayName("Nucleus")
        mainClass("io.github.nucleuspowered.nucleus.bootstrap.NucleusBootstrapper")
        description("Nucleus")
        version(versionString)
        links {
            homepage("https://nucleuspowered.org")
            source("https://github.com/NucleusPowered/Nucleus")
            issues("https://github.com/NucleusPowered/Nucleus/issues")
        }
        contributor("dualspiral") {
            description("Lead Developer")
        }
        dependency("spongeapi") {
            loadOrder(org.spongepowered.plugin.metadata.PluginDependency.LoadOrder.AFTER)
            optional(false)
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

repositories {
    mavenCentral()
    maven("https://repo.spongepowered.org/maven")
}

dependencies {
    implementation(project(":nucleus-api"))
    implementation(project(":nucleus-core"))
    implementation(project(":nucleus-bootstrap"))
    testImplementation("junit:junit:4.12")
}

/**
 * For use with Github Actions, as you probably suspected by the name of the task
 */
val setGithubActionsVersion by tasks.registering(Exec::class) {
    commandLine("echo", "::set-env name=NUCLEUS_VERSION::$version")
    if (!level.isSnapshot) {
        commandLine("echo", "::set-env name=NUCLEUS_NOT_SNAPSHOT::true")
    }
}

val gitHash by tasks.registering(StdOutExec::class)  {
    commandLine("git", "rev-parse", "--short", "HEAD")
    doLast {
        project.extra["gitHash"] = result
    }
}

val gitCommitMessage by tasks.registering(StdOutExec::class) {
    commandLine("git", "log", "-1", "--format=%B")
}

val cleanOutput by tasks.registering(Delete::class) {
    delete(fileTree("output").matching {
        include("*.jar")
        include("*.md")
        include("*.json")
        include("*.yml")
    })
}

val copyJars by tasks.registering(Copy::class) {
    dependsOn(":nucleus-api:copyJars")
    dependsOn(":nucleus-core:build")
    dependsOn(tasks.shadowJar)
    from(tasks.shadowJar)
    into(project.file("output"))
}

val relNotes by tasks.registering(RelNotesTask::class) {
    dependsOn(gitHash)
    dependsOn(gitCommitMessage)
    versionString { versionString }
    gitHash { gitHash.get().result!! }
    gitCommit { gitCommitMessage.get().result!! }
    level { level }
}

val writeRelNotes by tasks.registering {
    dependsOn(relNotes)
    doLast {
        Files.write(project.projectDir.toPath().resolve("changelogs").resolve("${nucVersion}.md"),
                relNotes.get().relNotes!!.toByteArray(StandardCharsets.UTF_8))
    }
}

val outputRelNotes by tasks.registering {
    dependsOn(relNotes)
    doLast {
        Files.write(project.projectDir.toPath().resolve("output").resolve("${nucVersion}.md"),
                relNotes.get().relNotes!!.toByteArray(StandardCharsets.UTF_8))
    }
}

val shadowJar: com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar by tasks

val upload by tasks.registering(io.github.nucleuspowered.gradle.task.UploadToOre::class) {
    dependsOn(shadowJar)
    dependsOn(relNotes)
    fileProvider = shadowJar.archiveFile
    notes = { relNotes.get().relNotes!! }
    releaseLevel = { level }
    apiKey = properties["ore_apikey"]?.toString() ?: System.getenv("NUCLEUS_ORE_APIKEY")
    pluginid = "nucleus"
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "1.8"
}

val setupDocGen by tasks.registering(io.github.nucleuspowered.gradle.task.SetupServer::class) {
    dependsOn(shadowJar)
    spongeVanillaDownload = URL("https://repo.spongepowered.org/maven/org/spongepowered/spongevanilla/1.12.2-7.2.3-RC372/spongevanilla-1.12.2-7.2.3-RC372.jar")
    spongeVanillaSHA1Hash = "e72ec4bc0368cd2fc604f412eed5fa8941d7580c"
    acceptEula = true
    fileProvider = shadowJar.archiveFile
}

val runDocGen by tasks.registering(JavaExec::class) {
    dependsOn(setupDocGen)
    workingDir = File("run")
    classpath = files(File("run/sv.jar"))
    systemProperty("nucleus.docgen", "docs")
}

val copyDocGen by tasks.registering(Copy::class) {
    dependsOn(runDocGen)
    from("run/docs")
    into(project.file("output"))
}

val docGen by tasks.registering {
    dependsOn(copyDocGen)
    dependsOn(runDocGen)
    dependsOn(setupDocGen)
}

val deleteDocGenServer by tasks.registering(Delete::class) {
    delete("run")
}

tasks {
    shadowJar {
        dependsOn(":nucleus-api:build")
        dependsOn(":nucleus-core:build")
        dependsOn(":nucleus-bootstrap:build")
        dependsOn(gitHash)
        doFirst {
            manifest {
                attributes["Implementation-Title"] = project.name
                attributes["SpongeAPI-Version"] = project.properties["spongeApiVersion"]
                attributes["Implementation-Version"] = project.version
                attributes["Git-Hash"] = gitHash.get().result
            }
        }

        dependencies {
            include(project(":nucleus-api"))
            include(project(":nucleus-core"))
            include(project(":nucleus-modules"))
            include(project(":nucleus-bootstrap"))
            include(dependency("io.vavr:vavr:0.10.3"))
        }

        if (!project.properties.containsKey("norelocate")) {
            relocate("io.vavr", "io.github.nucleuspowered.relocate.io.vavr")
        }

        exclude("io/github/nucleuspowered/nucleus/api/NucleusAPIMod.class")
        val minecraftVersion = project.properties["minecraftversion"]
        archiveFileName.set("Nucleus-${versionString}-MC${minecraftVersion}-$filenameSuffix-plugin.jar")
    }

    build {
        dependsOn(shadowJar)
        dependsOn(copyJars)
        dependsOn(outputRelNotes)
    }

    clean {
        dependsOn(cleanOutput)
    }

    blossomSourceReplacementJava {
        dependsOn(gitHash)
    }

}

publishing {
    publications {
        create<MavenPublication>("core") {
            shadow.component(this)
            setArtifacts(listOf(shadowJar))
            version = versionString
            groupId = project.properties["groupId"]?.toString()!!
            artifactId = project.properties["artifactId"]?.toString()!!
        }
    }

    repositories {
        if (!versionString.contains("SNAPSHOT")) {
            maven {
                name = "GitHubPackages"
                url = uri(project.findProperty("gpr.uri") as String? ?: "${project.properties["ghUri"]?.toString()!!}${System.getenv("REPO")}")
                credentials {
                    username = project.findProperty("gpr.user") as String? ?: System.getenv("USER")
                    password = project.findProperty("gpr.key") as String? ?: System.getenv("KEY")
                }
            }
        }
    }
}

