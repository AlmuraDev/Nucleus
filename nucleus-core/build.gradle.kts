val spongeApiVersion: String by rootProject
val vavrVersion: String by rootProject

dependencies {
    api(project(":nucleus-api"))
    annotationProcessor("org.spongepowered:spongeapi:$spongeApiVersion")
    api("io.vavr:vavr:$vavrVersion")
}
