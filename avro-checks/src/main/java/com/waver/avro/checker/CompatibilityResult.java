package com.waver.avro.checker;

import java.util.List;

/**
 * Holds the outcome of a compatibility check.
 *
 * @param compatible        {@code true} when the schemas are compatible
 * @param incompatibilities human-readable messages describing each incompatibility;
 *                          empty when {@code compatible} is {@code true}
 */
public record CompatibilityResult(boolean compatible, List<String> incompatibilities) {

    /** Returns a compatible result with an empty incompatibilities list. */
    public static CompatibilityResult ok() {
        return new CompatibilityResult(true, List.of());
    }

    /** Returns an incompatible result with the given list of reasons. */
    public static CompatibilityResult fail(List<String> reasons) {
        return new CompatibilityResult(false, List.copyOf(reasons));
    }
}
