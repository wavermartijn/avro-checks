package com.waver.avro.cli;

import com.waver.avro.advice.MigrationAdvice;
import com.waver.avro.checker.AvroCompatibilityChecker;
import com.waver.avro.checker.CompatibilityLevel;
import com.waver.avro.schema.AvroChecks;
import org.apache.avro.Schema;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class Main {

    static final String VERSION = loadVersion();

    private static String loadVersion() {
        try (InputStream is = Main.class.getClassLoader().getResourceAsStream("VERSION")) {
            if (is == null) return "unknown";
            return new String(is.readAllBytes(), StandardCharsets.UTF_8).strip();
        } catch (IOException e) {
            return "unknown";
        }
    }

    private static final String HELP = """
        avro-checks-cli  v""" + VERSION + """

        Compares two Avro schemas and checks compatibility.
        When incompatibilities are found, step-by-step migration advice is printed.

        Usage:
          avro-checks-cli [--help]
          avro-checks-cli <new-schema-json> <previous-schema-json> [LEVEL]
          avro-checks-cli -f <new-schema.json> <previous-schema.json> [LEVEL]

        Options:
          --help   Show this help message and exit
          -f       Read schemas from files instead of inline JSON strings

        LEVEL (default: BACKWARD):
          NONE                 No compatibility check
          BACKWARD             New schema can read data written by the previous schema
          BACKWARD_TRANSITIVE  New schema can read data written by ALL previous schemas
          FORWARD              Previous schema can read data written by the new schema
          FORWARD_TRANSITIVE   ALL previous schemas can read data written by the new schema
          FULL                 Both BACKWARD and FORWARD (previous schema only)
          FULL_TRANSITIVE      Both BACKWARD and FORWARD (ALL previous schemas)

        Exit codes:
          0  Compatible
          1  Incompatible (migration advice is printed)
          2  Usage or parse error

        Repository : https://github.com/wavermartijn/avro-checks
        Owners     : Martijn van der Pauw
        """;

    public static void main(String[] args) {
        System.exit(run(args));
    }

    /** Returns the exit code (0 = compatible, 1 = incompatible, 2 = usage error). */
    static int run(String[] args) {
        if (args.length > 0 && "--help".equals(args[0])) {
            System.out.println(HELP);
            return 0;
        }

        boolean fileMode = args.length > 0 && "-f".equals(args[0]);
        String[] rest    = fileMode ? Arrays.copyOfRange(args, 1, args.length) : args;

        if (rest.length < 2) {
            System.err.println(HELP);
            return 2;
        }

        String newSchemaJson;
        String prevSchemaJson;

        if (fileMode) {
            try {
                newSchemaJson  = Files.readString(Path.of(rest[0])).strip();
                prevSchemaJson = Files.readString(Path.of(rest[1])).strip();
            } catch (IOException e) {
                System.err.println("Failed to read schema file: " + e.getMessage());
                return 2;
            }
        } else {
            newSchemaJson  = rest[0];
            prevSchemaJson = rest[1];
        }

        String levelArg = rest.length >= 3 ? rest[2].toUpperCase() : "BACKWARD";

        CompatibilityLevel level;
        try {
            level = CompatibilityLevel.valueOf(levelArg);
        } catch (IllegalArgumentException e) {
            System.err.println("Unknown compatibility level: " + levelArg);
            System.err.println(HELP);
            return 2;
        }

        Schema newSchema;
        Schema prevSchema;
        try {
            newSchema  = AvroChecks.parseSchema(newSchemaJson);
            prevSchema = AvroChecks.parseSchema(prevSchemaJson);
        } catch (Exception e) {
            System.err.println("Failed to parse schema: " + e.getMessage());
            return 2;
        }

        List<MigrationAdvice> advice =
            AvroCompatibilityChecker.checkWithAdvice(newSchema, prevSchema, level);

        System.out.println("avro-checks-cli v" + VERSION);
        System.out.println("Compatibility level : " + level);
        System.out.println("New schema          : " + newSchema.getFullName());
        System.out.println("Previous schema     : " + prevSchema.getFullName());

        if (advice.isEmpty()) {
            System.out.println("Result              : COMPATIBLE");
            return 0;
        } else {
            System.out.println("Result              : INCOMPATIBLE");
            System.out.println();
            for (int i = 0; i < advice.size(); i++) {
                System.out.println("--- Incompatibility " + (i + 1) + " ---");
                System.out.print(advice.get(i));
            }
            return 1;
        }
    }
}
