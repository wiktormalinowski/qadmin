package pl.maliniak.qadmin.runtime;

import java.util.Map;

public class DisplayRegistry {
    private final Map<String, String> displayKeys;

    public DisplayRegistry(Map<String, String> displayKeys) {
        this.displayKeys = displayKeys;
    }

    /**
     * Returns the attribute to display for a given class or field.
     */
    public String getDisplayAttribute(String className, String fieldName) {
        if (fieldName != null && !fieldName.isEmpty()) {
            String fieldKey = className + "." + fieldName;
            if (displayKeys.containsKey(fieldKey)) {
                return displayKeys.get(fieldKey);
            }
        }
        return displayKeys.get(className);
    }

    /**
     * Returns the attribute to display for a given class.
     */
    public String getDisplayAttribute(String className) {
        return getDisplayAttribute(className, null);
    }
}
