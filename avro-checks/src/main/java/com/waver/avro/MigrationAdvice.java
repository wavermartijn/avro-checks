package com.waver.avro;

import java.util.List;

/**
 * A detected incompatibility paired with concrete, step-by-step migration advice.
 */
public record MigrationAdvice(String incompatibility, List<String> steps) {

    public MigrationAdvice {
        steps = List.copyOf(steps);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Issue   : ").append(incompatibility).append(System.lineSeparator());
        sb.append("Advice  :").append(System.lineSeparator());
        for (int i = 0; i < steps.size(); i++) {
            sb.append("  ").append(i + 1).append(". ").append(steps.get(i))
              .append(System.lineSeparator());
        }
        return sb.toString();
    }
}
