package pl.maliniak.qadmin.runtime;

import java.util.Set;

public class ExclusionRegistry {
    private final Set<String> excludedKeys;

    public ExclusionRegistry(Set<String> excludedKeys) {
        this.excludedKeys = excludedKeys;
    }

    /**
     * Returns true if the class and field are NOT excluded.
     */
    public boolean isIncluded(String className, String fieldName) {
        if (excludedKeys.contains(className)) {
            return false;
        }
        return fieldName == null || fieldName.isEmpty() || !excludedKeys.contains(className + "." + fieldName);
    }

    /**
     * Returns true if the class is NOT excluded.
     */
    public boolean isIncluded(String className) {
        return isIncluded(className, null);
    }
}