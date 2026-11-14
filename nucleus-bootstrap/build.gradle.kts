import org.apache.tools.ant.filters.ReplaceTokens

val spongeApiVersion: String by rootProject
val declaredApiVersion: String by rootProject
val validVersions: String? by rootProject

dependencies {
    api(project(":nucleus-core"))
    api(project(":nucleus-modules"))
    annotationProcessor("org.spongepowered:spongeapi:$spongeApiVersion")
}

val gitHash: Provider<String> = providers.exec {
    commandLine("git", "rev-parse", "--short", "HEAD")
    isIgnoreExitValue = true
}.standardOutput.asText.map { it.trim().ifEmpty { "unknown" } }

// NucleusPluginInfo holds @token@ placeholders for the build details
val generatePluginInfo by tasks.registering(Copy::class) {
    val tokens = mapOf(
        "name" to rootProject.name,
        "version" to rootProject.version.toString(),
        "description" to (rootProject.findProperty("description")?.toString() ?: ""),
        "url" to (rootProject.findProperty("url")?.toString() ?: ""),
        "gitHash" to gitHash.get(),
        "spongeversion" to declaredApiVersion,
        "validversions" to (validVersions ?: declaredApiVersion)
    )
    inputs.properties(tokens)
    from("src/main/java-templates")
    into(layout.buildDirectory.dir("generated/sources/pluginInfo"))
    filter<ReplaceTokens>("tokens" to tokens)
}

sourceSets.main {
    java.srcDir(generatePluginInfo)
}

tasks.processResources {
    val props = mapOf(
        "version" to rootProject.version.toString(),
        "description" to (rootProject.findProperty("description")?.toString() ?: ""),
        // A bare version is a soft requirement in plugin-meta, so this also accepts the server's 21.0.0-RC builds
        "spongeApiVersion" to declaredApiVersion.substringBefore('-') + ".0"
    )
    inputs.properties(props)
    filesMatching("META-INF/sponge_plugins.json") {
        expand(props)
    }
}
