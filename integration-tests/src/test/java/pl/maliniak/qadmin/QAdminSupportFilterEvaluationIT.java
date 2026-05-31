package pl.maliniak.qadmin;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import pl.maliniak.qadmin.runtime.QAdminSupport;
import pl.maliniak.qadmin.runtime.AiQueryResponse;

import java.util.Map;
import java.util.List;
import pl.maliniak.qadmin.runtime.AiQueryResponse.Filter;
import pl.maliniak.qadmin.runtime.AiQueryResponse.Order;
import pl.maliniak.qadmin.runtime.AiQueryResponse.Operator;
import pl.maliniak.qadmin.runtime.AiQueryResponse.Direction;

@QuarkusTest
public class QAdminSupportFilterEvaluationIT extends AbstractEvaluationIT {

    @Inject
    QAdminSupport qAdminSupport;

    @InjectMock
    EntityManager entityManager;

    @Test
    public void evaluateFilterExtraction() {
        String availableColumns = "name (String), age (Integer), price (Double), isActive (Boolean)";

        Map<String, AiQueryResponse> samples = Map.of(
                "Show me users with name John", new AiQueryResponse(
                        List.of(new Filter("name", Operator.EQUALS, "John")),
                        List.of()
                ),
                "List products where price is greater than 100", new AiQueryResponse(
                        List.of(new Filter("price", Operator.GREATER_THAN, "100")),
                        List.of()
                ),
                "Get active users ordered by age descending", new AiQueryResponse(
                        List.of(new Filter("isActive", Operator.EQUALS, "true")),
                        List.of(new Order("age", Direction.DESC))
                )
        );

        evaluateModel(
            "Filter & Order Extraction",
            samples,
            input -> qAdminSupport.getFiltersAndOrders(input, availableColumns),
            (expected, actual) -> {
                if (actual == null) return false;
                var expFilters = expected.filters() != null ? expected.filters() : List.of();
                var actFilters = actual.filters() != null ? actual.filters() : List.of();
                var expOrders = expected.orders() != null ? expected.orders() : List.of();
                var actOrders = actual.orders() != null ? actual.orders() : List.of();
                return expFilters.equals(actFilters) && expOrders.equals(actOrders);
            }
        );
    }
}
