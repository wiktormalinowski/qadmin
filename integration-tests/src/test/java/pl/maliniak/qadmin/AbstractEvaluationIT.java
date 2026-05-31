package pl.maliniak.qadmin;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Assertions;

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

        String modelId = ConfigProvider.getConfig()
                .getOptionalValue("quarkus.langchain4j.ollama.chat-model.model-id", String.class)
                .orElse("Unknown Model");
        
        updateRanking(modelId, testName, precision, averageSpeed);
        
        Assertions.assertEquals(total, passed, "Evaluation did not reach 100% precision. Passed: " + passed + " / " + total);
    }

    private synchronized void updateRanking(String modelId, String testName, double precision, double averageSpeed) {
        try {
            Path rankingPath = Paths.get("../model-ranking.md");
            List<String> lines = new ArrayList<>();
            if (Files.exists(rankingPath)) {
                lines = Files.readAllLines(rankingPath);
            }

            int headerIndex = -1;
            for (int i = 0; i < lines.size(); i++) {
                if (lines.get(i).startsWith("| Model |")) {
                    headerIndex = i;
                    break;
                }
            }

            if (headerIndex == -1) {
                // Initialize new file
                lines.clear();
                lines.add("# Model Evaluation Rankings");
                lines.add("");
                lines.add("| Model | Last Updated |");
                lines.add("|---|---|");
                headerIndex = 2;
            }

            String headerLine = lines.get(headerIndex);
            List<String> headers = new ArrayList<>(Arrays.asList(headerLine.split("\\|")));
            if (!headers.isEmpty() && headers.get(0).trim().isEmpty()) headers.remove(0);
            if (!headers.isEmpty() && headers.get(headers.size() - 1).trim().isEmpty()) headers.remove(headers.size() - 1);
            for (int i = 0; i < headers.size(); i++) headers.set(i, headers.get(i).trim());

            String precisionCol = testName + " Precision";
            String latencyCol = testName + " Latency";

            if (!headers.contains(precisionCol)) headers.add(precisionCol);
            if (!headers.contains(latencyCol)) headers.add(latencyCol);

            // Reconstruct header and separator
            lines.set(headerIndex, "| " + String.join(" | ", headers) + " |");
            
            StringBuilder separator = new StringBuilder("|");
            for (int i = 0; i < headers.size(); i++) separator.append("---|");
            
            if (lines.size() <= headerIndex + 1 || !lines.get(headerIndex + 1).startsWith("|---")) {
                lines.add(headerIndex + 1, separator.toString());
            } else {
                lines.set(headerIndex + 1, separator.toString());
            }

            // Process rows
            int modelRowIndex = -1;
            for (int i = headerIndex + 2; i < lines.size(); i++) {
                String row = lines.get(i);
                if (row.trim().isEmpty()) continue;
                List<String> columns = new ArrayList<>(Arrays.asList(row.split("\\|")));
                if (!columns.isEmpty() && columns.get(0).trim().isEmpty()) columns.remove(0);
                if (!columns.isEmpty() && columns.get(columns.size() - 1).trim().isEmpty()) columns.remove(columns.size() - 1);
                for (int j = 0; j < columns.size(); j++) columns.set(j, columns.get(j).trim());

                if (columns.get(0).equals(modelId)) {
                    modelRowIndex = i;
                    while (columns.size() < headers.size()) columns.add("-");
                    
                    columns.set(1, LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                    columns.set(headers.indexOf(precisionCol), String.format("%.2f%%", precision));
                    columns.set(headers.indexOf(latencyCol), String.format("%.2f ms", averageSpeed));
                    
                    lines.set(i, "| " + String.join(" | ", columns) + " |");
                } else {
                    while (columns.size() < headers.size()) columns.add("-");
                    lines.set(i, "| " + String.join(" | ", columns) + " |");
                }
            }

            if (modelRowIndex == -1) {
                // Add new model row
                List<String> newRow = new ArrayList<>();
                for (int i = 0; i < headers.size(); i++) {
                    if (headers.get(i).equals("Model")) newRow.add(modelId);
                    else if (headers.get(i).equals("Last Updated")) newRow.add(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                    else if (headers.get(i).equals(precisionCol)) newRow.add(String.format("%.2f%%", precision));
                    else if (headers.get(i).equals(latencyCol)) newRow.add(String.format("%.2f ms", averageSpeed));
                    else newRow.add("-");
                }
                lines.add("| " + String.join(" | ", newRow) + " |");
            }

            Files.write(rankingPath, lines);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
