plugins {
    `java-library`
}

dependencies {
    api("org.apache.avro:avro:1.11.3")

    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}
