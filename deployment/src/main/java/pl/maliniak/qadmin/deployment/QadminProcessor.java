package pl.maliniak.qadmin.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import pl.maliniak.qadmin.runtime.AdminResource;

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
    void devService(BuildProducer<RunTimeConfigurationDefaultBuildItem> config) {
        // Odpowiednik quarkus.quinoa.ui-dir w properties
        config.produce(new RunTimeConfigurationDefaultBuildItem(
                "quarkus.quinoa.ui-dir", "../runtime/src/main/webui"));

        // Odpowiednik quarkus.quinoa.ui-root-path
        config.produce(new RunTimeConfigurationDefaultBuildItem(
                "quarkus.quinoa.ui-root-path", "qadmin-ui"));

        // Automatyczna instalacja node/npm
        config.produce(new RunTimeConfigurationDefaultBuildItem(
                "quarkus.quinoa.package-manager-install", "true"));
    }
}
