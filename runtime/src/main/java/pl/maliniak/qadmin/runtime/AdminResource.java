package pl.maliniak.qadmin.runtime;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.EntityType;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Path("/q/qadmin/api")
@Produces(MediaType.APPLICATION_JSON)
public class AdminResource {

    @Inject
    Instance<EntityManager> iEm;

    @GET
    @Path("/entities")
    public List<String> getEntities() {
        if (iEm.isResolvable()) {
            return iEm.get().getMetamodel().getEntities().stream()
                    .map(EntityType::getName)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}