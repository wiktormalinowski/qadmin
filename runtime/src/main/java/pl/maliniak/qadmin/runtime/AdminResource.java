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
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Path("/q/qadmin/api")
@Produces(MediaType.APPLICATION_JSON)
public class AdminResource {

    @Inject
    Instance<EntityManager> iEm;

    @Inject
    ExclusionRegistry registry;

    @GET
    @Path("/entities")
    public List<String> getEntities() {
        if (iEm.isResolvable()) {
            return iEm.get().getMetamodel().getEntities().stream()
                    .filter(e -> {
                        String fullClassName = e.getJavaType().getName();
                        return !registry.isExcluded(fullClassName, "");
                    })
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
                        (e.getName().equals(entityName) ||
                                e.getJavaType().getSimpleName().equals(entityName)) && !registry.isExcluded(entityName, null)   // nazwa klasy
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

    @GET
    @Path("/data/{entityName}")
    public List<Object> getData(@PathParam("entityName") String entityName) {
        if (!iEm.isResolvable()) {
            return Collections.emptyList();
        }

        var em = iEm.get();

        return em.getMetamodel()
                .getEntities()
                .stream()
                .filter(e ->
                        e.getName().equals(entityName) ||
                                e.getJavaType().getSimpleName().equals(entityName)
                )
                .findFirst()
                // 1. Sprawdzenie czy cała KLASA nie jest wykluczona
                .filter(e -> !registry.isExcluded(e.getJavaType().getName(), ""))
                .map(entityType -> {
                    Class<?> clazz = entityType.getJavaType();
                    CriteriaBuilder cb = em.getCriteriaBuilder();

                    // Używamy Tuple, aby zachować nazwy kolumn w wyniku
                    CriteriaQuery<Tuple> query = cb.createTupleQuery();
                    Root<?> root = query.from(clazz);

                    // 2. Filtrowanie pól przy użyciu Registry
                    // Używamy getSingularAttributes, aby uniknąć problemów z kolekcjami (@OneToMany) w prostym SELECT
                    List<Selection<?>> selections = entityType.getSingularAttributes().stream()
                            .filter(attr -> !registry.isExcluded(clazz.getName(), attr.getName()))
                            .map(attr -> root.get(attr.getName()).alias(attr.getName()))
                            .collect(Collectors.toList());

                    // Jeśli wszystkie pola zostały wykluczone, zwracamy pustą listę
                    if (selections.isEmpty()) {
                        return Collections.<Object>emptyList();
                    }

                    query.multiselect(selections);

                    // 3. Pobranie danych i mapowanie Tuple -> Map<String, Object>
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
                })
                .orElse(Collections.emptyList());
    }
}