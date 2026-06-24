plugins {
    java
}

val projectVersion: String = rootProject.file("VERSION").readText().trim()

subprojects {
    group = "com.waver.avro"
    version = projectVersion

    repositories {
        mavenCentral()
    }

    if (!name.contains("quarkus")) {
        apply(plugin = "java")

        java {
            toolchain {
                languageVersion = JavaLanguageVersion.of(21)
            }
        }

        dependencies {
            testRuntimeOnly("org.junit.platform:junit-platform-launcher")
        }

        tasks.withType<Test> {
            useJUnitPlatform()
        }
    }
}
