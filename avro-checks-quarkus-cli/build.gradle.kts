plugins {
    java
    id("io.quarkus") version "3.27.1"
}

dependencies {
    implementation(enforcedPlatform("io.quarkus.platform:quarkus-bom:3.27.1"))
    implementation("io.quarkus:quarkus-picocli")
    implementation(project(":avro-checks"))

    testImplementation("io.quarkus:quarkus-junit5")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

quarkus {
    // Quarkus configuration
    setFinalName("avro-checks-quarkus-cli")
}

tasks.processResources {
    filesMatching("application.properties") {
        expand("projectVersion" to project.version)
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

