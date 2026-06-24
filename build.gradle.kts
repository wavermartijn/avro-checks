plugins {
    java
}

val projectVersion: String = rootProject.file("VERSION").readText().trim()

subprojects {
    apply(plugin = "java")

    group = "com.waver.avro"
    version = projectVersion

    repositories {
        mavenCentral()
    }

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
