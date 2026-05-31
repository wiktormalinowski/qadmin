package pl.maliniak.qadmin.runtime;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Meta-annotation used to mark annotations that should be automatically
 * scanned and added to the QAdminMetadataRegistry.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface QAdminMeta {
}
