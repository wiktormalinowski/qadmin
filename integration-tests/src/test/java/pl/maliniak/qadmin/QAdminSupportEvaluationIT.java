package pl.maliniak.qadmin;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import pl.maliniak.qadmin.runtime.QAdminSupport;

import java.util.Map;

@QuarkusTest
public class QAdminSupportEvaluationIT extends AbstractEvaluationIT {

    @Inject
    QAdminSupport qAdminSupport;

    @InjectMock
    EntityManager entityManager;

    @Test
    public void evaluateModelSpeedAndPrecision() {
        // We mock EntityManager just to prevent any database interactions during this evaluation.
        String availableEntities = "Place, User, Order, Article, Comment, Phone, Laptop, Tablet, Cake";

        // Define our test samples: Input -> Expected Entity
        Map<String, String> samples = Map.of(
                "Get me all cakes", "Cake",
                "Show me articles", "Article",
                "List all TVs", "-",
                "Find the user John", "User",
                "Show me places", "Place"
        );

        evaluateModel(
            "Entity Name Extraction", 
            samples, 
            input -> qAdminSupport.getEntityName(input, availableEntities), 
            (expected, actual) -> expected.equalsIgnoreCase(actual != null ? actual.trim() : "")
        );
    }
}
