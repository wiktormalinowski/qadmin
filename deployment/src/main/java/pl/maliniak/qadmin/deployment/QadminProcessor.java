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
import pl.maliniak.qadmin.runtime.ExcludeQAdmin;
import pl.maliniak.qadmin.runtime.ExclusionRecorder;
import pl.maliniak.qadmin.runtime.ExclusionRegistry;
import pl.maliniak.qadmin.runtime.DisplayQAdmin;
import pl.maliniak.qadmin.runtime.DisplayRecorder;
import pl.maliniak.qadmin.runtime.DisplayRegistry;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
    SyntheticBeanBuildItem scanAndRegister(
            CombinedIndexBuildItem indexBuildItem,
            ExclusionRecorder recorder) {

        Set<String> foundExclusions = new HashSet<>();
        var index = indexBuildItem.getIndex();
        var excludeDotName = DotName.createSimple(ExcludeQAdmin.class.getName());

        for (var annotation : index.getAnnotations(excludeDotName)) {
            var target = annotation.target();

            if (target.kind() == org.jboss.jandex.AnnotationTarget.Kind.CLASS) {
                foundExclusions.add(target.asClass().name().toString());
            }
            else if (target.kind() == org.jboss.jandex.AnnotationTarget.Kind.FIELD) {
                var field = target.asField();
                foundExclusions.add(field.declaringClass().name() + "." + field.name());
            }
        }

        return SyntheticBeanBuildItem.configure(ExclusionRegistry.class)
                .scope(jakarta.enterprise.context.ApplicationScoped.class)
                .runtimeValue(recorder.createRegistry(foundExclusions))
                .done();
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    SyntheticBeanBuildItem scanAndRegisterDisplay(
            CombinedIndexBuildItem indexBuildItem,
            DisplayRecorder recorder) {

        Map<String, String> foundDisplays = new HashMap<>();
        var index = indexBuildItem.getIndex();
        var displayDotName = DotName.createSimple(DisplayQAdmin.class.getName());

        for (var annotation : index.getAnnotations(displayDotName)) {
            var target = annotation.target();
            var value = annotation.value().asString();

            if (target.kind() == org.jboss.jandex.AnnotationTarget.Kind.CLASS) {
                foundDisplays.put(target.asClass().name().toString(), value);
            }
            else if (target.kind() == org.jboss.jandex.AnnotationTarget.Kind.FIELD) {
                var field = target.asField();
                foundDisplays.put(field.declaringClass().name() + "." + field.name(), value);
            }
        }

        return SyntheticBeanBuildItem.configure(DisplayRegistry.class)
                .scope(jakarta.enterprise.context.ApplicationScoped.class)
                .runtimeValue(recorder.createRegistry(foundDisplays))
                .done();
    }
}
