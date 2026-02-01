package pl.maliniak.qadmin.runtime;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Selection;
import jakarta.persistence.metamodel.EntityType;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.util.*;
import java.util.stream.Collectors;

@Path("/q/qadmin/api")
@Produces(MediaType.APPLICATION_JSON)
public class AdminResource {

    @Inject
    Instance<EntityManager> iEm;

    @Inject
    ExclusionRegistry registry;

    @Inject
    QAdminSupport support;

    /**
     * Returns all included entity names.
     */
    @GET
    @Path("/entities")
    public List<String> getEntities() {
        if (!iEm.isResolvable()) return Collections.emptyList();
        return getIncludedEntities().stream()
                .map(EntityType::getName)
                .collect(Collectors.toList());
    }

    /**
     * Returns metadata for a single included entity.
     */
    @GET
    @Path("/entityMetadata/{entityName}")
    public List<String> getEntityMetadata(@PathParam("entityName") String entityName) {
        EntityType<?> entityType = findIncludedEntity(entityName);
        if (entityType == null) return Collections.emptyList();
        return entityType.getAttributes().stream()
                .map(attr -> attr.getName() + " : " + attr.getJavaType().getSimpleName())
                .collect(Collectors.toList());
    }

    @POST
    @Path("/ai/listWithMetadata")
    public Map<String, Object> getListWithMetadata(PromptRequest promptRequest) {
        String entityName = support.getEntityName(promptRequest.prompt(), getEntities().toString()).trim();
        List<Object> data = getData(entityName);
        List<String > metadata = getEntityMetadata(entityName);
        Map<String, Object> map = new HashMap<>();
        map.put("name", entityName);
        map.put("data", data);
        map.put("metadata", metadata);
        return map;

    }

    /**
     * Returns data for a single included entity.
     */
    @GET
    @Path("/data/{entityName}")
    public List<Object> getData(@PathParam("entityName") String entityName) {
        EntityType<?> entityType = findIncludedEntity(entityName);
        if (entityType == null) return Collections.emptyList();
        var em = iEm.get();
        Class<?> clazz = entityType.getJavaType();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<?> root = query.from(clazz);

        List<Selection<?>> selections = entityType.getSingularAttributes().stream()
                .filter(attr -> registry.isIncluded(clazz.getName(), attr.getName()))
                .map(attr -> root.get(attr.getName()).alias(attr.getName()))
                .collect(Collectors.toList());

        if (selections.isEmpty()) return Collections.emptyList();

        query.multiselect(selections);

        return em.createQuery(query)
                .getResultList()
                .stream()
                .map(tuple -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (jakarta.persistence.TupleElement<?> el : tuple.getElements()) {
                        row.put(el.getAlias(), tuple.get(el));
                    }
                    return (Object) row;
                })
                .collect(Collectors.toList());
    }

    private List<EntityType<?>> getIncludedEntities() {
        if (!iEm.isResolvable()) return Collections.emptyList();
        return iEm.get().getMetamodel().getEntities().stream()
                .filter(e -> registry.isIncluded(e.getJavaType().getName()))
                .collect(Collectors.toList());
    }

    private EntityType<?> findIncludedEntity(String entityName) {
        if (!iEm.isResolvable()) return null;
        return iEm.get().getMetamodel().getEntities().stream()
                .filter(e -> (e.getName().equals(entityName) ||
                        e.getJavaType().getSimpleName().equals(entityName)) &&
                        registry.isIncluded(e.getJavaType().getName()))
                .findFirst()
                .orElse(null);
    }
}