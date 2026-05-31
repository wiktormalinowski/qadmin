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
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.transaction.Transactional;

import java.util.*;


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
                .toList();
    }

    public Map<String, Object> getEntityView(String entityName) {
        return getEntityView(entityName, null);
    }

    public Map<String, Object> getEntityView(String entityName, AiQueryResponse criteria) {
        EntityType<?> entityType = findIncludedEntity(entityName);
        if (entityType == null) {
            return null;
        }
        Map<String, Object> metadata = getEntityMetadata(entityName);
        List<Object> data = getData(entityName, criteria);
        
        Map<String, Object> map = new HashMap<>();
        map.put("name", entityName);
        map.put("metadata", metadata);
        map.put("data", data);
        return map;
    }

    public Map<String, Object> queryAi(PromptRequest promptRequest) {
        String entityName = support.getEntityName(promptRequest.prompt(), getEntityNames().toString()).trim();
        EntityType<?> entityType = findIncludedEntity(entityName);
        if (entityType == null) {
            return null;
        }

        String columns = entityType.getSingularAttributes().stream()
                .map(attr -> attr.getName() + " (" + attr.getJavaType().getSimpleName() + ")")
                .collect(Collectors.joining(", "));

        AiQueryResponse criteria = null;
        try {
            criteria = support.getFiltersAndOrders(promptRequest.prompt(), columns);
        } catch (Exception e) {
            // ignore or log
            e.printStackTrace();
        }

        return getEntityView(entityName, criteria);
    }

    @Transactional
    public boolean deleteData(String entityName, String id) {
        EntityType<?> entityType = findIncludedEntity(entityName);
        if (entityType == null) return false;

        var em = iEm.get();
        Class<?> clazz = entityType.getJavaType();

        SingularAttribute<?, ?> idAttribute = entityType.getSingularAttributes().stream()
                .filter(SingularAttribute::isId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Entity has no ID attribute"));

        Object parsedId;
        try {
            parsedId = parseValue(id, idAttribute.getJavaType());
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

    private List<Object> getData(String entityName, AiQueryResponse criteria) {
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
                .toList();

        if (selections.isEmpty()) return Collections.emptyList();

        query.multiselect(selections);

        if (criteria != null) {
            // Apply Filters
            if (criteria.filters() != null && !criteria.filters().isEmpty()) {
                List<Predicate> predicates = new ArrayList<>();
                for (AiQueryResponse.Filter filter : criteria.filters()) {
                    try {
                        Path<Object> path = root.get(filter.column());
                        Class<?> javaType = path.getJavaType();
                        Object value = parseValue(filter.value(), javaType);
                        
                        switch (filter.operator()) {
                            case EQUALS -> predicates.add(cb.equal(path, value));
                            case NOT_EQUALS -> predicates.add(cb.notEqual(path, value));
                            case GREATER_THAN -> {
                                if (Comparable.class.isAssignableFrom(javaType)) {
                                    Expression<Comparable> compPath = (Expression<Comparable>) (Object) path;
                                    predicates.add(cb.greaterThan(compPath, (Comparable) value));
                                }
                            }
                            case LESS_THAN -> {
                                if (Comparable.class.isAssignableFrom(javaType)) {
                                    Expression<Comparable> compPath = (Expression<Comparable>) (Object) path;
                                    predicates.add(cb.lessThan(compPath, (Comparable) value));
                                }
                            }
                            case LIKE -> predicates.add(cb.like(cb.lower(path.as(String.class)), "%" + String.valueOf(value).toLowerCase() + "%"));
                        }
                    } catch (Exception e) {
                        // ignore invalid filters
                    }
                }
                if (!predicates.isEmpty()) {
                    query.where(cb.and(predicates.toArray(new Predicate[0])));
                }
            }

            // Apply Orders
            if (criteria.orders() != null && !criteria.orders().isEmpty()) {
                List<Order> jpaOrders = new ArrayList<>();
                for (AiQueryResponse.Order order : criteria.orders()) {
                    try {
                        if (AiQueryResponse.Direction.DESC.equals(order.direction())) {
                            jpaOrders.add(cb.desc(root.get(order.column())));
                        } else {
                            jpaOrders.add(cb.asc(root.get(order.column())));
                        }
                    } catch (Exception e) {
                        // ignore invalid orders
                    }
                }
                if (!jpaOrders.isEmpty()) {
                    query.orderBy(jpaOrders);
                }
            }
        }

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
                .toList();
    }

    private List<EntityType<?>> getIncludedEntities() {
        if (!iEm.isResolvable()) return Collections.emptyList();
        return iEm.get().getMetamodel().getEntities().stream()
                .filter(e -> !metadataRegistry.hasMetadata(ExcludeQAdmin.class, e.getJavaType().getName()))
                .toList();
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

    private Object parseValue(String value, Class<?> type) {
        if (value == null) return null;
        return switch (type.getSimpleName()) {
            case "String" -> value;
            case "Integer", "int" -> Integer.parseInt(value);
            case "Long", "long" -> Long.parseLong(value);
            case "Boolean", "boolean" -> Boolean.parseBoolean(value);
            case "Double", "double" -> Double.parseDouble(value);
            case "UUID" -> UUID.fromString(value);
            default -> value;
        };
    }
}
