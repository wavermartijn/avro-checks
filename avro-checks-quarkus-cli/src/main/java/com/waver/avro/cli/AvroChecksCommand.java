package com.waver.avro.cli;

import com.waver.avro.advice.MigrationAdvice;
import com.waver.avro.checker.AvroCompatibilityChecker;
import com.waver.avro.checker.CompatibilityCheckBuilder;
import com.waver.avro.checker.CompatibilityLevel;
import com.waver.avro.schema.AvroChecks;
import org.apache.avro.Schema;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Quarkus Picocli command for avro-checks CLI.
 * Supports native compilation with GraalVM.
 */
@CommandLine.Command(
    name = "avro-checks",
    mixinStandardHelpOptions = true,
    version = "avro-checks 0.0.1-RC1",
    description = "Check Avro schema compatibility with migration advice.",
    header = {
        "@|green avro-checks|@ - Avro Schema Compatibility Checker",
        "Repository: https://github.com/wavermartijn/avro-checks",
        "Owner: Martijn van der Pauw",
        ""
    }
)
public class AvroChecksCommand implements Callable<Integer> {

    @CommandLine.Parameters(
        index = "0",
        description = "New schema (JSON string or file path with -f flag)"
    )
    private String newSchemaInput;

    @CommandLine.Parameters(
        index = "1",
        description = "Previous schema (JSON string or file path with -f flag)"
    )
    private String previousSchemaInput;

    @CommandLine.Option(
        names = {"-l", "--level"},
        description = "Compatibility level: ${COMPLETION-CANDIDATES}",
        defaultValue = "BACKWARD"
    )
    private CompatibilityLevel level;

    @CommandLine.Option(
        names = {"-f", "--file"},
        description = "Read schemas from files instead of inline JSON"
    )
    private boolean fileMode;

    @CommandLine.Option(
        names = {"--with-history"},
        description = "Additional historical schemas (for transitive checks)",
        split = ","
    )
    private List<String> historyInputs;

    @Override
    public Integer call() throws Exception {
        String newSchemaJson = readSchema(newSchemaInput);
        String prevSchemaJson = readSchema(previousSchemaInput);

        Schema newSchema;
        Schema prevSchema;
        try {
            newSchema = AvroChecks.parseSchema(newSchemaJson);
            prevSchema = AvroChecks.parseSchema(prevSchemaJson);
        } catch (Exception e) {
            System.err.println("Failed to parse schema: " + e.getMessage());
            return 2;
        }

        // Build the check
        CompatibilityCheckBuilder builder = AvroCompatibilityChecker.check()
            .forCandidate(newSchema)
            .withCompatibility(level)
            .withOlderSchema(prevSchema);

        // Add history if provided
        if (historyInputs != null && !historyInputs.isEmpty()) {
            for (String historyInput : historyInputs) {
                String historyJson = fileMode ? readFile(historyInput) : historyInput;
                Schema historySchema = AvroChecks.parseSchema(historyJson);
                builder.withOlderSchema(historySchema);
            }
        }

        List<MigrationAdvice> advice = builder.checkWithAdvice();

        System.out.println("Compatibility level : " + level);
        System.out.println("New schema          : " + newSchema.getFullName());
        System.out.println("Previous schema     : " + prevSchema.getFullName());

        if (advice.isEmpty()) {
            System.out.println("Result              : @|green COMPATIBLE|@");
            return 0;
        } else {
            System.out.println("Result              : @|red INCOMPATIBLE|@");
            System.out.println();
            for (int i = 0; i < advice.size(); i++) {
                System.out.println("--- Incompatibility " + (i + 1) + " ---");
                System.out.print(advice.get(i));
            }
            return 1;
        }
    }

    private String readSchema(String input) throws IOException {
        if (fileMode) {
            return readFile(input);
        }
        return input;
    }

    private String readFile(String path) throws IOException {
        return Files.readString(Path.of(path)).strip();
    }
}
