val spongeApiVersion: String by rootProject

sourceSets {
    main {
        resources {
            exclude("assets/nucleus/suggestions/**")
        }
    }
}

dependencies {
    api(project(":nucleus-api"))
    implementation(project(":nucleus-core"))
    annotationProcessor("org.spongepowered:spongeapi:$spongeApiVersion")
}
