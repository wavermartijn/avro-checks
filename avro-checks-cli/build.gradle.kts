plugins {
    application
}

dependencies {
    implementation(project(":avro-checks"))
    runtimeOnly("org.slf4j:slf4j-nop:1.7.36")

    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

application {
    mainClass = "com.waver.avro.cli.Main"
}

tasks.processResources {
    from(rootProject.file("VERSION"))
}

tasks.jar {
    archiveBaseName = "avro-checks-cli"
    archiveVersion  = project.version.toString()
    manifest {
        attributes["Main-Class"]             = "com.waver.avro.cli.Main"
        attributes["Implementation-Title"]   = "avro-checks-cli"
        attributes["Implementation-Version"] = project.version.toString()
    }
    dependsOn(configurations.runtimeClasspath)
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
