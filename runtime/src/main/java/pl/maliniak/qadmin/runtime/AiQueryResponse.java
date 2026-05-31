package pl.maliniak.qadmin.runtime;

import java.util.List;
import dev.langchain4j.model.output.structured.Description;

public record AiQueryResponse(
        @Description("List of filters to apply to the query. Must be exactly 'filters'.")
        List<Filter> filters,
        
        @Description("List of orders to sort the query results. Must be exactly 'orders', NOT 'ordering'.")
        List<Order> orders
) {
    public enum Operator {
        EQUALS, NOT_EQUALS, GREATER_THAN, LESS_THAN, LIKE
    }

    public enum Direction {
        ASC, DESC
    }

    public record Filter(
            @Description("The exact name of the column to filter on. Must be exactly 'column', NOT 'field'.")
            String column,
            
            @Description("The filtering operator to use. MUST be strictly uppercase: EQUALS, NOT_EQUALS, GREATER_THAN, LESS_THAN, LIKE.")
            Operator operator,
            
            @Description("The value to filter by.")
            String value
    ) {}

    public record Order(
            @Description("The exact name of the column to sort by. Must be exactly 'column', NOT 'field'.")
            String column,
            
            @Description("The direction to sort in. MUST be strictly uppercase: ASC or DESC.")
            Direction direction
    ) {}
}
