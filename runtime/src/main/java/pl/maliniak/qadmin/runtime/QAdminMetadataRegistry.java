package pl.maliniak.qadmin.runtime;

import java.util.Map;

public class QAdminMetadataRegistry {
    // Map of AnnotationName -> (Class/Field Key -> Value)
    private final Map<String, Map<String, String>> metadata;

    public QAdminMetadataRegistry(Map<String, Map<String, String>> metadata) {
        this.metadata = metadata;
    }

    public String getMetadata(String annotationName, String className, String fieldName) {
        Map<String, String> annMeta = metadata.get(annotationName);
        if (annMeta == null) return null;
        
        if (fieldName != null && !fieldName.isEmpty()) {
            String fieldKey = className + "." + fieldName;
            if (annMeta.containsKey(fieldKey)) {
                return annMeta.get(fieldKey);
            }
        }
        return annMeta.get(className);
    }

    public String getMetadata(Class<?> annotationClass, String className, String fieldName) {
        return getMetadata(annotationClass.getSimpleName(), className, fieldName);
    }

    public String getMetadata(Class<?> annotationClass, String className) {
        return getMetadata(annotationClass.getSimpleName(), className);
    }
    
    public boolean hasMetadata(Class<?> annotationClass, String className, String fieldName) {
        return hasMetadata(annotationClass.getSimpleName(), className, fieldName);
    }

    public boolean hasMetadata(Class<?> annotationClass, String className) {
        return hasMetadata(annotationClass.getSimpleName(), className);
    }

    public String getMetadata(String annotationName, String className) {
        return getMetadata(annotationName, className, null);
    }
    
    public boolean hasMetadata(String annotationName, String className, String fieldName) {
        return getMetadata(annotationName, className, fieldName) != null;
    }

    public boolean hasMetadata(String annotationName, String className) {
        return getMetadata(annotationName, className) != null;
    }
}
