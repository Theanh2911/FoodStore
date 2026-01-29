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
            // 1. Get all products from database (including inactive for AI suggestion)
            List<Product> products = productService.getAllProductsIncludingInactive();
            
            // 2. Build menu string from products
            String menuString = buildMenuString(products);
            
            // 3. Build system prompt with dynamic menu
            String systemPrompt = buildSystemPrompt(menuString);
            
            // 4. Create messages list
            List<GroqMessage> messages = new ArrayList<>();
            messages.add(new GroqMessage("system", systemPrompt));
            messages.add(new GroqMessage("user", userDemand));
            
            // 5. Create request
            GroqRequest groqRequest = new GroqRequest(messages);
            
            // 6. Call Groq API
            GroqResponse groqResponse = callGroqAPI(groqRequest);
            
            // 7. Extract and parse the suggestion
            return extractMenuSuggestion(groqResponse);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate menu suggestion: " + e.getMessage(), e);
        }
    }

    private String buildMenuString(List<Product> products) {
        // Group products by category name, only take one product per category
        Map<String, List<Product>> groupedByCategory = products.stream()
                .collect(Collectors.groupingBy(p -> p.getCategory().getName()));
        
        StringBuilder menuBuilder = new StringBuilder();
        
        // Build menu organized by category
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
        return "Bạn là AI agent gợi ý món ăn cho nhà hàng.\n\n" +
                "⚠️ QUY TẮC BẮT BUỘC (KHÔNG ĐƯỢC VI PHẠM):\n" +
                "- Chỉ sử dụng các món có trong MENU.\n" +
                "- main_dish PHẢI chọn từ category **Đồ ăn**\n" +
                "- side_dish PHẢI chọn từ category **Đồ ăn thêm**\n" +
                "- drink PHẢI chọn từ category **Đồ uống**\n" +
                "- Nếu category nào không có món → để giá trị null\n" +
                "- CHỈ trả về MỘT JSON hợp lệ.\n" +
                "- KHÔNG giải thích, KHÔNG markdown, KHÔNG thêm bất kỳ text nào ngoài JSON.\n" +
                "- Nếu vi phạm format JSON → tự sửa lại cho đúng.\n\n" +
                "🎯 FORMAT JSON (BẮT BUỘC):\n" +
                "{\n" +
                "  \"main_dish\": string hoặc null,\n" +
                "  \"side_dish\": string hoặc null,\n" +
                "  \"drink\": string hoặc null,\n" +
                "  \"reason\": string\n" +
                "}\n\n" +
                "🧠 LOGIC GỢI Ý:\n" +
                "- Phân loại món theo category trong menu\n" +
                "- main_dish: chọn món từ **Đồ ăn**\n" +
                "- side_dish: chọn món từ **Đồ ăn thêm**\n" +
                "- drink: chọn món từ **Đồ uống**\n" +
                "- Ưu tiên combo phù hợp với yêu cầu người dùng\n\n" +
                "📋 MENU:\n" +
                menuString + "\n\n" +
                "📌 VÍ DỤ OUTPUT ĐÚNG:\n" +
                "{\n" +
                "  \"main_dish\": \"Cơm sườn nướng\",\n" +
                "  \"side_dish\": \"Canh rong biển\",\n" +
                "  \"drink\": \"Trà đào\",\n" +
                "  \"reason\": \"Combo cân bằng dinh dưỡng, phù hợp khẩu vị\"\n" +
                "}\n";
    }

    private GroqResponse callGroqAPI(GroqRequest request) {
        try {
            // Create headers
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + groqApiKey);
            headers.set("Content-Type", "application/json");
            
            // Create HTTP entity
            HttpEntity<GroqRequest> entity = new HttpEntity<>(request, headers);
            
            // Call API
            ResponseEntity<GroqResponse> response = restTemplate.exchange(
                    GROQ_API_URL,
                    HttpMethod.POST,
                    entity,
                    GroqResponse.class
            );
            
            return response.getBody();
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to call Groq API: " + e.getMessage(), e);
        }
    }

    private MenuSuggestion extractMenuSuggestion(GroqResponse response) {
        try {
            if (response == null || response.choices() == null || response.choices().isEmpty()) {
                throw new RuntimeException("Invalid response from Groq API");
            }
            
            // Get the content from first choice
            String content = response.choices().get(0).message().content();
            
            // Parse JSON content to MenuSuggestion
            return objectMapper.readValue(content, MenuSuggestion.class);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse menu suggestion: " + e.getMessage(), e);
        }
    }
}
