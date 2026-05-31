package pl.maliniak.qadmin.runtime;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class QAdminWarmup {

    private static final Logger LOG = Logger.getLogger(QAdminWarmup.class);

    @Inject
    QAdminSupport qAdminSupport;

    void onStart(@Observes StartupEvent ev) {
        LOG.info("Warming up QAdmin AI service...");
        try {
            // Send a dummy request to trigger LLM model loading and Quarkus lazy initialization
            long start = System.currentTimeMillis();
            qAdminSupport.getEntityName("warmup", "WarmupEntity");
            long end = System.currentTimeMillis();
            LOG.infof("QAdmin AI service warmed up in %d ms.", (end - start));
        } catch (Exception e) {
            LOG.warn("Failed to warm up AI service. It may be temporarily unavailable.", e);
        }
    }
}
