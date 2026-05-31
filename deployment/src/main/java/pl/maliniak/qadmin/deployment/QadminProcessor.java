package pl.maliniak.qadmin.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import org.jboss.jandex.DotName;
import pl.maliniak.qadmin.runtime.AdminResource;
import pl.maliniak.qadmin.runtime.QAdminMeta;
import pl.maliniak.qadmin.runtime.QAdminMetadataRecorder;
import pl.maliniak.qadmin.runtime.QAdminMetadataRegistry;

import java.util.HashMap;
import java.util.Map;

class QadminProcessor {

    private static final String FEATURE = "qadmin";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }
    
    @BuildStep
    void registerResource(BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(AdminResource.class));
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    SyntheticBeanBuildItem scanAndRegisterMetadata(
            CombinedIndexBuildItem indexBuildItem,
            QAdminMetadataRecorder recorder) {

        Map<String, Map<String, String>> metadata = new HashMap<>();
        var index = indexBuildItem.getIndex();
        var qAdminMetaDotName = DotName.createSimple(QAdminMeta.class.getName());

        for (var metaAnnotationInstance : index.getAnnotations(qAdminMetaDotName)) {
            var targetAnnotation = metaAnnotationInstance.target().asClass();
            String fullAnnName = targetAnnotation.name().toString();
            String annotationName = fullAnnName.substring(fullAnnName.lastIndexOf('.') + 1);
            
            Map<String, String> specificMetadata = new HashMap<>();
            
            for (var annotation : index.getAnnotations(targetAnnotation.name())) {
                var target = annotation.target();
                String value = "true";
                
                var valueAttr = annotation.value();
                if (valueAttr != null) {
                    value = valueAttr.asString();
                }

                if (target.kind() == org.jboss.jandex.AnnotationTarget.Kind.CLASS) {
                    specificMetadata.put(target.asClass().name().toString(), value);
                }
                else if (target.kind() == org.jboss.jandex.AnnotationTarget.Kind.FIELD) {
                    var field = target.asField();
                    specificMetadata.put(field.declaringClass().name() + "." + field.name(), value);
                }
            }
            metadata.put(annotationName, specificMetadata);
        }

        return SyntheticBeanBuildItem.configure(QAdminMetadataRegistry.class)
                .scope(jakarta.enterprise.context.ApplicationScoped.class)
                .runtimeValue(recorder.createRegistry(metadata))
                .done();
    }
}
