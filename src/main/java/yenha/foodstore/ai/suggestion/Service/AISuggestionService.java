package yenha.foodstore.ai.suggestion.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import yenha.foodstore.Menu.Entity.Product;
import yenha.foodstore.Menu.Service.ProductService;
import yenha.foodstore.ai.suggestion.DTO.*;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AISuggestionService {

    @Autowired
    private ProductService productService;

    @Value("${groq.api.key}")
    private String groqApiKey;

    private static final String GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MenuSuggestion getSuggestion(String userDemand) {
        try {
            List<Product> products = productService.getAllProductsIncludingInactive();

            String menuString = buildMenuString(products);

            String systemPrompt = buildSystemPrompt(menuString);

            List<GroqMessage> messages = new ArrayList<>();
            messages.add(new GroqMessage("system", systemPrompt));
            messages.add(new GroqMessage("user", userDemand));

            GroqRequest groqRequest = new GroqRequest(messages);

            GroqResponse groqResponse = callGroqAPI(groqRequest);

            return extractMenuSuggestion(groqResponse);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate menu suggestion: " + e.getMessage(), e);
        }
    }

    private String buildMenuString(List<Product> products) {
        Map<String, List<Product>> groupedByCategory = products.stream()
                .collect(Collectors.groupingBy(p -> p.getCategory().getName()));
        
        StringBuilder menuBuilder = new StringBuilder();

        for (Map.Entry<String, List<Product>> entry : groupedByCategory.entrySet()) {
            String categoryName = entry.getKey();
            List<Product> categoryProducts = entry.getValue();
            
            menuBuilder.append("\n**").append(categoryName).append(":**\n");
            for (Product product : categoryProducts) {
                menuBuilder.append("- ").append(product.getName()).append("\n");
            }
        }
        
        return menuBuilder.toString();
    }

    private String buildSystemPrompt(String menuString) {
        return "You are an AI food recommendation expert for the 'FoodStore' restaurant system.\n" +
                "Your Task: Analyze the USER REQUEST and recommend a food combo from the provided MENU DATA.\n\n" +

                "### MENU DATA ###\n" +
                "The available menu items are listed below within XML tags:\n" +
                "<menu_data>\n" +
                menuString + "\n" +
                "</menu_data>\n\n" +

                "### CRITICAL RULES (MUST FOLLOW) ###\n" +
                "1. **Strict Extraction**: You must select items EXACTLY as they appear in <menu_data>. Do NOT translate, rename, or invent new dishes.\n" +
                "2. **Categorization**:\n" +
                "   - `main_dish`: Select from category 'Đồ ăn' or 'Món chính'.\n" +
                "   - `side_dish`: Select from category 'Đồ ăn thêm' or 'Khai vị'.\n" +
                "   - `drink`: Select from category 'Đồ uống'.\n" +
                "3. **Null Handling**: If a category has no suitable item or is missing from the menu, you MUST return `null`.\n" +
                "4. **Output Format**: Return RAW JSON only. Do NOT use Markdown formatting (no ```json blocks). Do NOT add conversational text.\n\n" +

                "### RESPONSE JSON STRUCTURE ###\n" +
                "{\n" +
                "  \"main_dish\": \"Exact item name from menu (or null)\",\n" +
                "  \"side_dish\": \"Exact item name from menu (or null)\",\n" +
                "  \"drink\": \"Exact item name from menu (or null)\",\n" +
                "  \"reason\": \"A short explanation (under 20 words) IN VIETNAMESE explaining why this combo fits the request.\"\n" +
                "}\n\n" +

                "### FEW-SHOT EXAMPLES ###\n" +
                "User: 'Tôi muốn ăn trưa nhanh gọn.'\n" +
                "AI Output: {\"main_dish\": \"Cơm gà xối mỡ\", \"side_dish\": \"Canh rong biển\", \"drink\": \"Trà đá\", \"reason\": \"Bữa trưa đầy đặn, phục vụ nhanh, giải nhiệt tốt.\"}\n\n" +

                "User: 'Chỉ uống nước thôi.'\n" +
                "AI Output: {\"main_dish\": null, \"side_dish\": null, \"drink\": \"Cà phê sữa\", \"reason\": \"Theo yêu cầu chỉ gọi đồ uống.\"}\n";
    }

    private GroqResponse callGroqAPI(GroqRequest request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + groqApiKey);
            headers.set("Content-Type", "application/json");

            HttpEntity<GroqRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<GroqResponse> response = restTemplate.exchange(
                    GROQ_API_URL,
                    HttpMethod.POST,
                    entity,
                    GroqResponse.class
            );
            
            return response.getBody();
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to call Groq: " + e.getMessage(), e);
        }
    }

    private MenuSuggestion extractMenuSuggestion(GroqResponse response) {
        try {
            if (response == null || response.choices() == null || response.choices().isEmpty()) {
                throw new RuntimeException("Invalid response from Groq");
            }

            String content = response.choices().get(0).message().content();

            return objectMapper.readValue(content, MenuSuggestion.class);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse menu suggestion: " + e.getMessage(), e);
        }
    }
}
