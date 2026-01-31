package pl.maliniak.qadmin.runtime;

import java.util.Set;

public class ExclusionRegistry {
    private final Set<String> excludedKeys;

    public ExclusionRegistry(Set<String> excludedKeys) {
        this.excludedKeys = excludedKeys;
    }

    public boolean isExcluded(String className, String fieldName) {
        return excludedKeys.contains(className) ||
                excludedKeys.contains(className + "." + fieldName);
    }
}