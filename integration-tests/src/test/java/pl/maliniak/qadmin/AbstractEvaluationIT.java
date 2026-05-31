package pl.maliniak.qadmin;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

public abstract class AbstractEvaluationIT {

    protected <T> void evaluateModel(String testName, Map<String, T> samples, Function<String, T> aiFunction, BiFunction<T, T, Boolean> matchFunction) {
        System.out.println("==================================================");
        System.out.println("Starting AI Model Evaluation: " + testName);
        System.out.println("==================================================");

        int passed = 0;
        int total = samples.size();
        long totalExecutionTime = 0;

        for (Map.Entry<String, T> entry : samples.entrySet()) {
            String input = entry.getKey();
            T expected = entry.getValue();

            long startTime = System.currentTimeMillis();
            
            T actual = null;
            String errorMessage = null;
            try {
                actual = aiFunction.apply(input);
            } catch(Exception e) {
                errorMessage = "ERROR: " + e.getMessage();
            }
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            totalExecutionTime += duration;

            boolean isCorrect = errorMessage == null && matchFunction.apply(expected, actual);
            if (isCorrect) {
                passed++;
            }

            System.out.printf("Input: '%s'\n", input);
            if (errorMessage != null) {
                System.out.printf("Expected: '%s' | Actual: '%s'\n", expected, errorMessage);
            } else {
                System.out.printf("Expected: '%s' | Actual: '%s'\n", expected, actual);
            }
            System.out.printf("Result: %s | Time: %d ms\n", isCorrect ? "PASS" : "FAIL", duration);
            System.out.println("--------------------------------------------------");
        }

        double precision = ((double) passed / total) * 100;
        double averageSpeed = (double) totalExecutionTime / total;

        System.out.printf("EVALUATION COMPLETE\n");
        System.out.printf("Precision Score: %.2f%%\n", precision);
        System.out.printf("Average Latency: %.2f ms per request\n", averageSpeed);
        System.out.println("==================================================");
        
        org.junit.jupiter.api.Assertions.assertEquals(total, passed, "Evaluation did not reach 100% precision. Passed: " + passed + " / " + total);
    }
}
