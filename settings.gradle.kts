rootProject.name = "avro-checks-root"

val includeQuarkus = providers.gradleProperty("includeQuarkus").orElse("true").get() == "true"

if (includeQuarkus) {
    include("avro-checks", "avro-checks-cli", "avro-checks-quarkus-cli")
} else {
    include("avro-checks", "avro-checks-cli")
}
