package com.waver.avro.checker;

import java.util.List;

public record CompatibilityResult(boolean compatible, List<String> incompatibilities) {

    public static CompatibilityResult ok() {
        return new CompatibilityResult(true, List.of());
    }

    public static CompatibilityResult fail(List<String> reasons) {
        return new CompatibilityResult(false, List.copyOf(reasons));
    }
}
