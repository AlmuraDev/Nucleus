description = "The Ultimate Essentials Plugin API."

val spongeApiVersion: String by rootProject

dependencies {
    api("org.spongepowered:spongeapi:$spongeApiVersion")
    annotationProcessor("org.spongepowered:spongeapi:$spongeApiVersion")
}
