plugins {
    `java-library`
    id("com.gradleup.shadow") version "9.2.2"
}

val nucleusVersion: String by project
val nucleusVersionSuffix: String? by project
val spongeApiVersion: String by project
val minecraftversion: String by project
val declaredApiVersion: String by project

version = if (nucleusVersionSuffix.isNullOrBlank()) nucleusVersion else "$nucleusVersion-$nucleusVersionSuffix"
group = "io.github.nucleuspowered"

allprojects {
    apply(plugin = "java-library")
    group = rootProject.group
    version = rootProject.version

    extensions.configure<JavaPluginExtension> {
        toolchain.languageVersion.set(JavaLanguageVersion.of(25))
    }

    dependencies {
        // SpongeAPI 21 no longer exposes these. Guava ships with the game; Checker annotations are only needed to
        // compile and will be replaced by jspecify (what SpongeAPI itself uses now).
        "compileOnly"("com.google.guava:guava:33.3.1-jre")
        "compileOnly"("org.checkerframework:checker-qual:3.48.4")
        "testImplementation"("junit:junit:4.13.2")
        "testImplementation"("org.mockito:mockito-core:5.20.0")
        "testImplementation"("com.google.guava:guava:33.3.1-jre")
        "testCompileOnly"("org.checkerframework:checker-qual:3.48.4")
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(25)
        // Show every error, not just the first 100
        options.compilerArgs.addAll(listOf("-Xmaxerrs", "100000", "-Xmaxwarns", "0"))
    }
}

dependencies {
    implementation(project(":nucleus-bootstrap"))
}

// The plugin jar: every Nucleus project plus vavr (relocated). Sponge, Guice, Configurate, Adventure, Gson and
// Guava come from the server.
tasks.shadowJar {
    archiveFileName.set("Nucleus-$version-MC$minecraftversion-SpongeAPI$declaredApiVersion-plugin.jar")
    dependencies {
        include(project(":nucleus-api"))
        include(project(":nucleus-core"))
        include(project(":nucleus-modules"))
        include(project(":nucleus-bootstrap"))
        include(dependency("io.vavr:.*"))
    }
    relocate("io.vavr", "io.github.nucleuspowered.relocate.io.vavr")
    exclude("io/github/nucleuspowered/nucleus/api/NucleusAPIMod.class")
    manifest {
        attributes["Implementation-Title"] = "Nucleus"
        attributes["Implementation-Version"] = project.version
        attributes["SpongeAPI-Version"] = spongeApiVersion
    }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
