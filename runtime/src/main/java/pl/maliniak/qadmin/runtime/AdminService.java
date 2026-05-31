package pl.maliniak.qadmin.runtime;

import jakarta.enterprise.context.ApplicationScoped;
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

import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class AdminService {

    @Inject
    Instance<EntityManager> iEm;

    @Inject
    QAdminMetadataRegistry metadataRegistry;

    @Inject
    QAdminSupport support;

    public List<String> getEntityNames() {
        if (!iEm.isResolvable()) return Collections.emptyList();
        return getIncludedEntities().stream()
                .map(EntityType::getName)
                .collect(Collectors.toList());
    }

    public Map<String, Object> getEntityView(String entityName) {
        EntityType<?> entityType = findIncludedEntity(entityName);
        if (entityType == null) {
            return null;
        }
        Map<String, Object> metadata = getEntityMetadata(entityName);
        List<Object> data = getData(entityName);
        
        Map<String, Object> map = new HashMap<>();
        map.put("name", entityName);
        map.put("metadata", metadata);
        map.put("data", data);
        return map;
    }

    public Map<String, Object> queryAi(PromptRequest promptRequest) {
        String entityName = support.getEntityName(promptRequest.prompt(), getEntityNames().toString()).trim();
        return getEntityView(entityName);
    }

    @Transactional
    public boolean deleteData(String entityName, String id) {
        EntityType<?> entityType = findIncludedEntity(entityName);
        if (entityType == null) return false;

        var em = iEm.get();
        Class<?> clazz = entityType.getJavaType();

        SingularAttribute<?, ?> idAttribute = null;
        for (SingularAttribute<?, ?> attr : entityType.getSingularAttributes()) {
            if (attr.isId()) {
                idAttribute = attr;
                 break;
            }
        }

        if (idAttribute == null) throw new IllegalArgumentException("Entity has no ID attribute");

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
            throw new IllegalArgumentException("Invalid ID format");
        }

        Object entity = em.find(clazz, parsedId);
        if (entity == null) {
            return false;
        }

        em.remove(entity);
        return true;
    }

    private Map<String, Object> getEntityMetadata(String entityName) {
        EntityType<?> entityType = findIncludedEntity(entityName);
        if (entityType == null) return Collections.emptyMap();
        Class<?> clazz = entityType.getJavaType();
        
        Map<String, Object> metadata = new HashMap<>();
        for (var attr : entityType.getAttributes()) {
            Map<String, String> fieldMeta = new HashMap<>();
            fieldMeta.put("javaType", attr.getJavaType().getSimpleName());
            
            if (attr.isAssociation()) {
                String displayField = metadataRegistry.getMetadata(DisplayQAdmin.class, clazz.getName(), attr.getName());
                if (displayField == null) {
                    displayField = metadataRegistry.getMetadata(DisplayQAdmin.class, attr.getJavaType().getName());
                }
                if (displayField != null) {
                    fieldMeta.put("displayAttribute", displayField);
                }
            }
            metadata.put(attr.getName(), fieldMeta);
        }
        return metadata;
    }

    private List<Object> getData(String entityName) {
        EntityType<?> entityType = findIncludedEntity(entityName);
        if (entityType == null) return Collections.emptyList();
        var em = iEm.get();
        Class<?> clazz = entityType.getJavaType();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<?> root = query.from(clazz);

        List<Selection<?>> selections = entityType.getSingularAttributes().stream()
                .filter(attr -> !metadataRegistry.hasMetadata(ExcludeQAdmin.class, clazz.getName(), attr.getName()))
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
                .filter(e -> !metadataRegistry.hasMetadata(ExcludeQAdmin.class, e.getJavaType().getName()))
                .collect(Collectors.toList());
    }

    private EntityType<?> findIncludedEntity(String entityName) {
        if (!iEm.isResolvable()) return null;
        return iEm.get().getMetamodel().getEntities().stream()
                .filter(e -> (e.getName().equals(entityName) ||
                        e.getJavaType().getSimpleName().equals(entityName)) &&
                        !metadataRegistry.hasMetadata(ExcludeQAdmin.class, e.getJavaType().getName()))
                .findFirst()
                .orElse(null);
    }
}
