package pl.maliniak.qadmin.runtime;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.EntityType;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
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

    @GET
    @Path("/entityMetadata/{entityName}")
    public List<String> getEntityMetadata(@PathParam("entityName") String entityName) {
        if (!iEm.isResolvable()) {
            return Collections.emptyList();
        }

        return iEm.get()
                .getMetamodel()
                .getEntities()
                .stream()
                .filter(e ->
                        e.getName().equals(entityName) ||                       // @Entity(name)
                                e.getJavaType().getSimpleName().equals(entityName)      // nazwa klasy
                )
                .findFirst()
                .map(entityType ->
                        entityType.getAttributes().stream()
                                .map(attr ->
                                        attr.getName() + " : " + attr.getJavaType().getSimpleName()
                                )
                                .collect(Collectors.toList())
                )
                .orElse(Collections.emptyList());
    }
}