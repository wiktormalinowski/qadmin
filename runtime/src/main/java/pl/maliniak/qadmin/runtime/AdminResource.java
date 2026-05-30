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
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.*;
import java.util.stream.Collectors;

@Path("/q/qadmin/api")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AdminResource {

    @Inject
    Instance<EntityManager> iEm;

    @Inject
    ExclusionRegistry registry;

    @Inject
    DisplayRegistry displayRegistry;

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
        Class<?> clazz = entityType.getJavaType();
        return entityType.getAttributes().stream()
                .map(attr -> {
                    String meta = attr.getName() + " : " + attr.getJavaType().getSimpleName();
                    if (attr.isAssociation()) {
                        String displayField = displayRegistry.getDisplayAttribute(clazz.getName(), attr.getName());
                        if (displayField == null) {
                            displayField = displayRegistry.getDisplayAttribute(attr.getJavaType().getName());
                        }
                        if (displayField != null) {
                            meta += " : " + displayField;
                        }
                    }
                    return meta;
                })
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

    @DELETE
    @Path("/data/{entityName}/{id}")
    @Transactional
    public Response deleteData(@PathParam("entityName") String entityName, @PathParam("id") String id) {
        EntityType<?> entityType = findIncludedEntity(entityName);
        if (entityType == null) return Response.status(404).build();

        var em = iEm.get();
        Class<?> clazz = entityType.getJavaType();

        SingularAttribute<?, ?> idAttribute = null;
        for (SingularAttribute<?, ?> attr : entityType.getSingularAttributes()) {
            if (attr.isId()) {
                idAttribute = attr;
                 break;
            }
        }

        if (idAttribute == null) return Response.status(400).entity("Entity has no ID attribute").build();

        Object parsedId;
        Class<?> idType = idAttribute.getJavaType();
        try {
            if (idType.equals(Long.class) || idType.equals(long.class)) {
                parsedId = Long.parseLong(id);
            } else if (idType.equals(Integer.class) || idType.equals(int.class)) {
                parsedId = Integer.parseInt(id);
            } else if (idType.equals(UUID.class)) {
                parsedId = UUID.fromString(id);
            } else {
                parsedId = id;
            }
        } catch (Exception e) {
            return Response.status(400).entity("Invalid ID format").build();
        }

        Object entity = em.find(clazz, parsedId);
        if (entity == null) {
            return Response.status(404).build();
        }

        em.remove(entity);
        return Response.noContent().build();
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