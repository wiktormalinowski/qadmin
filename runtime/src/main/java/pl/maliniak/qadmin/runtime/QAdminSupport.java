package pl.maliniak.qadmin.runtime;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import jakarta.inject.Singleton;

@RegisterAiService
@Singleton
public interface QAdminSupport {

//    @SystemMessage("""
//            Create a JPQL query to retrieve the data needed to answer the user’s question.
//            Identify the entity the user refers to (entity name may be approximate).
//            Output ONLY the JPQL query.
//            No markdown
//            No JSON
//            No backticks
//            No extra text
//            No semicolon at the end
//            No filtering unless the user asks for it
//            Query must be valid JPQL and use aliases
//            Query must be ready to use in code
//            example usage: dont use in generated output, alias is a first letter of entity name lowercase
//            INPUT --> "List all places"
//            OUTPUT --> "select p FROM Place p"
//
//            INPUT --> "get cakes"
//            OUTPUT --> "select c FROM Cake c"
//
//            INPUT --> "show me articles"
//            OUTPUT --> "select a FROM Article a"
//
//            Use only these entities: {entities}. If entity is not provided return '-'
//            INPUT --> "show me all tvs" theres no tv entity
//            OUTPUT --> "-"
//
//        """)
    @SystemMessage("""
            Identify the single entity name explicitly referenced in the user input
            (entity name may be approximate or in different language).
            Rules:
            You may ONLY choose from the provided list of available entities.
            Do NOT infer, guess, or map by category, type, or meaning.
            If no available entity name is mentioned, return -.
            Return ONLY the entity name or -.
            No explanations. No extra text.
            Examples:
            User Input: "Get me all cakes"
            Available entities: Place, User, Order
            Output: -
            User Input: "Show me articles"
            Available entities: Article, Comment, User
            Output: Article
            User Input: "List all TVs"
            Available entities: Phone, Laptop, Tablet
            Output: -
            
            Available entities: {entities}
            Output:
            """)
    String getEntityName(@UserMessage String question, String entities);

    @SystemMessage("""
            Extract filtering and ordering requirements from the user's question.
            Available columns with their java types: {columns}
            If no filters or orders, leave arrays empty.
            CRITICAL: All ENUM values (operator and direction) MUST be STRICTLY UPPERCASE (e.g. EQUALS, GREATER_THAN, ASC, DESC).
            CRITICAL: All values MUST be represented as STRINGS, even numbers and booleans (e.g. "true" instead of true, "100" instead of 100).
            MUST RETURN A VALID JSON OBJECT EXACTLY LIKE THIS EXAMPLE:
            {"filters": [{"column": "name", "operator": "EQUALS", "value": "abc"}], "orders": [{"column": "name", "direction": "ASC"}]}
            """)
    AiQueryResponse getFiltersAndOrders(@UserMessage String question, String columns);

}
