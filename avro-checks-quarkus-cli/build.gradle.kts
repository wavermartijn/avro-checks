plugins {
    java
    application
    id("io.quarkus") version "3.8.1"
}

dependencies {
    implementation(enforcedPlatform("io.quarkus.platform:quarkus-bom:3.8.1"))
    implementation("io.quarkus:quarkus-picocli")
    implementation(project(":avro-checks"))

    testImplementation("io.quarkus:quarkus-junit5")
}

application {
    mainClass.set("io.quarkus.bootstrap.runner.QuarkusEntryPoint")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

quarkus {
    // Quarkus configuration
    setFinalName("avro-checks-quarkus-cli")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// Native image task
tasks.register<Exec>("nativeBuild") {
    group = "build"
    description = "Build native image using GraalVM"
    dependsOn("quarkusBuild")

    val quarkusBuildDir = layout.buildDirectory.dir("quarkus-build").get().asFile

    // Run Quarkus native build
    commandLine(
        "sh", "-c",
        "${System.getenv("GRAALVM_HOME") ?: ""}/bin/native-image " +
        "-cp ${quarkusBuildDir}/lib/main/*.jar:${quarkusBuildDir}/quarkus-app/*.jar " +
        "-H:Name=avro-checks-quarkus-cli " +
        "-H:Class=io.quarkus.bootstrap.runner.QuarkusEntryPoint " +
        "--initialize-at-build-time=org.apache.avro.Schema\$Parser,org.apache.avro.Schema\$Type " +
        "--initialize-at-run-time=org.apache.avro.Schema " +
        "-H:+ReportExceptionStackTraces " +
        "-O2 " +
        "-march=native"
    )

    // Only run if GRAALVM_HOME is set
    doFirst {
        if (System.getenv("GRAALVM_HOME") == null) {
            throw GradleException("GRAALVM_HOME environment variable is not set. Please install GraalVM.")
        }
    }
}

// Simpler approach using Quarkus plugin
tasks.register<Exec>("buildNative") {
    group = "build"
    description = "Build native executable using Quarkus"

    // Use Quarkus CLI or Gradle plugin
    commandLine("./gradlew", "quarkusBuild", "-Dquarkus.package.type=native", "-x", "test")

    doFirst {
        println("Building native image with Quarkus...")
        println("This requires GraalVM to be installed and GRAALVM_HOME set")
    }
}
