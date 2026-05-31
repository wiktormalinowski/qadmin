package pl.maliniak.qadmin;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import pl.maliniak.qadmin.runtime.QAdminSupport;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
public class QAdminSupportEvaluationIT {

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

        System.out.println("==================================================");
        System.out.println("Starting AI Model Precision & Speed Evaluation");
        System.out.println("==================================================");

        int passed = 0;
        int total = samples.size();
        long totalExecutionTime = 0;

        for (Map.Entry<String, String> entry : samples.entrySet()) {
            String input = entry.getKey();
            String expected = entry.getValue();

            long startTime = System.currentTimeMillis();
            
            // Invoke the AI Service
            String actual = qAdminSupport.getEntityName(input, availableEntities);
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            totalExecutionTime += duration;

            boolean isCorrect = expected.equalsIgnoreCase(actual != null ? actual.trim() : "");
            if (isCorrect) {
                passed++;
            }

            System.out.printf("Input: '%s'\n", input);
            System.out.printf("Expected: '%s' | Actual: '%s'\n", expected, actual);
            System.out.printf("Result: %s | Time: %d ms\n", isCorrect ? "PASS" : "FAIL", duration);
            System.out.println("--------------------------------------------------");
        }

        double precision = ((double) passed / total) * 100;
        double averageSpeed = (double) totalExecutionTime / total;

        System.out.printf("EVALUATION COMPLETE\n");
        System.out.printf("Precision Score: %.2f%%\n", precision);
        System.out.printf("Average Latency: %.2f ms per request\n", averageSpeed);
        System.out.println("==================================================");
    }
}
