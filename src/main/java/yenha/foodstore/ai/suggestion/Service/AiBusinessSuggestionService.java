package yenha.foodstore.ai.suggestion.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import yenha.foodstore.Inventory.DTO.InventoryAggregateDTO;
import yenha.foodstore.Inventory.Repository.DailyProductInventoryRepository;
import yenha.foodstore.ai.suggestion.DTO.BusinessResponse.BusinessStrategyResponse;
import yenha.foodstore.ai.suggestion.DTO.ChoiceReponse.GroqMessage;
import yenha.foodstore.ai.suggestion.DTO.ChoiceReponse.GroqRequest;
import yenha.foodstore.ai.suggestion.DTO.ChoiceReponse.GroqResponse;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class AiBusinessSuggestionService {

    @Autowired
    private DailyProductInventoryRepository inventoryRepository;

    @Value("${groq.api.key}")
    private String groqApiKey;

    private static final String GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";
    
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private String buildBusinessStrategyPromptVFinal(String productDataString) {
        return "You are an AI business strategy advisor for the \"FoodStore\" restaurant system.\n" +
                "Your task is to ANALYZE PRODUCT PERFORMANCE DATA and propose FUTURE BUSINESS STRATEGIES.\n" +
                "You MUST strictly follow all rules below. Do NOT invent logic outside these rules.\n\n" +

                "### PRODUCT PERFORMANCE DATA ###\n" +
                "The product data is provided in JSON format inside XML tags:\n" +
                "<product_data>\n" +
                productDataString + "\n" +
                "</product_data>\n\n" +

                "### DATA FIELD MEANINGS ###\n" +
                "- soldRate: sales completion rate (0.0 → 1.0)\n" +
                "- profitPerUnit: profit earned per sold unit\n" +
                "- categoryName: product category (Món chính, Đồ ăn thêm, Đồ uống, ...)\n\n" +

                "### DEFINITIONS ###\n" +
                "- Best-selling product: soldRate >= 0.8\n" +
                "- Average product: 0.4 <= soldRate < 0.8\n" +
                "- Slow-selling product: soldRate < 0.4\n" +
                "- Profit level (high / low) MUST be determined RELATIVELY by comparing profitPerUnit among ALL products\n\n" +

                "### CATEGORY PRIORITY RULES ###\n" +
                "- \"Món chính\": highest priority for production and promotion decisions\n" +
                "- \"Đồ ăn thêm\": medium priority, mainly used for combos\n" +
                "- \"Đồ uống\": support items, do NOT aggressively increase production unless explicitly required\n\n" +

                "### STRATEGY RULES (STRICT) ###\n\n" +

                "1. Performance Tag:\n" +
                "   - soldRate >= 0.8 → best_seller\n" +
                "   - 0.4 <= soldRate < 0.8 → average\n" +
                "   - soldRate < 0.4 → slow_seller\n\n" +

                "2. Production Strategy:\n" +
                "   - best_seller → increase\n" +
                "   - slow_seller → decrease\n" +
                "   - average → keep\n" +
                "   - Exception: \"Đồ uống\" should usually keep production even if best_seller\n\n" +

                "3. Profit Margin Strategy:\n" +
                "   - High profitPerUnit + slow_seller → decrease profit margin\n" +
                "   - Low profitPerUnit + best_seller → increase profit margin\n" +
                "   - Otherwise → keep profit margin\n\n" +

                "4. Discontinue Rule (OVERRIDES ALL OTHER RULES):\n" +
                "   - If BOTH soldRate is slow_seller AND profitPerUnit is low →\n" +
                "     * Recommend discontinuing the product\n" +
                "     * productionStrategy = \"decrease\"\n" +
                "     * profitMarginStrategy = \"keep\"\n" +
                "     * promotionStrategy = null\n" +
                "     * note MUST clearly state \"đề xuất ngừng bán\"\n\n" +

                "5. Promotion & Combo Rules:\n" +
                "   - ONLY use products from the provided data\n" +
                "   - NEVER invent or rename products\n" +
                "   - A combo is ONLY created for a best-selling \"Món chính\"\n" +
                "   - Combo structure:\n" +
                "       + mainProduct: the best-selling \"Món chính\"\n" +
                "       + comboMainProduct: ONE DIFFERENT slow-selling \"Món chính\"\n" +
                "       + sideDish: ONE \"Đồ ăn thêm\"\n" +
                "       + drink: ONE \"Đồ uống\"\n\n" +

                "   IMPORTANT COMBO CONSTRAINTS:\n" +
                "   - comboMainProduct MUST be different from mainProduct\n" +
                "   - comboMainProduct MUST belong to category \"Món chính\"\n" +
                "   - If NO slow-selling \"Món chính\" exists:\n" +
                "       + comboMainProduct MUST be null\n" +
                "       + This reason MUST be briefly mentioned in note\n" +
                "   - If any required role has no suitable product, return null for that role\n\n" +

                "### OUTPUT RULES ###\n" +
                "- Return RAW JSON only\n" +
                "- Do NOT use Markdown\n" +
                "- Do NOT add explanations outside JSON\n" +
                "- Output MUST strictly follow the structure below\n\n" +

                "### RESPONSE JSON STRUCTURE ###\n" +
                "[\n" +
                "  {\n" +
                "    \"productId\": number,\n" +
                "    \"performanceTag\": \"best_seller | average | slow_seller\",\n" +
                "    \"productionStrategy\": \"increase | decrease | keep\",\n" +
                "    \"profitMarginStrategy\": \"increase | decrease | keep\",\n" +
                "    \"promotionStrategy\": {\n" +
                "      \"mainProduct\": \"Exact product name or null\",\n" +
                "      \"comboMainProduct\": \"Exact product name or null\",\n" +
                "      \"sideDish\": \"Exact product name or null\",\n" +
                "      \"drink\": \"Exact product name or null\"\n" +
                "    },\n" +
                "    \"note\": \"Short explanation in VIETNAMESE (max 20 words)\"\n" +
                "  }\n" +
                "]\n\n" +

                "### FINAL IMPORTANT NOTES ###\n" +
                "- Focus on PROFIT OPTIMIZATION and INVENTORY EFFICIENCY\n" +
                "- Be conservative and realistic\n" +
                "- Follow the rules exactly, even if the result seems less creative\n";
    }

    public List<BusinessStrategyResponse> getBusinessSuggestion(LocalDate startDate, LocalDate endDate) {
        try {
            // 1. Get aggregated inventory data
            List<InventoryAggregateDTO> inventoryData = inventoryRepository.getAggregatedInventory(startDate, endDate);
            
            if (inventoryData.isEmpty()) {
                throw new RuntimeException("No inventory data found for the specified date range");
            }
            
            // 2. Build JSON string from data
            String productDataString = buildProductDataString(inventoryData);
            
            // 3. Build system prompt
            String systemPrompt = buildBusinessStrategyPromptVFinal(productDataString);
            
            // 4. Prepare messages for Groq
            List<GroqMessage> messages = new ArrayList<>();
            messages.add(new GroqMessage("system", systemPrompt));
            messages.add(new GroqMessage("user", 
                "Analyze the provided product data and suggest business strategies for each product."));
            
            // 5. Call Groq API
            GroqRequest groqRequest = new GroqRequest(messages);
            GroqResponse groqResponse = callGroqAPI(groqRequest);
            
            // 6. Parse and return response
            return extractBusinessStrategies(groqResponse);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate business suggestions: " + e.getMessage(), e);
        }
    }

    private String buildProductDataString(List<InventoryAggregateDTO> inventoryData) {
        try {
            return objectMapper.writeValueAsString(inventoryData);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize product data: " + e.getMessage(), e);
        }
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
            throw new RuntimeException("Failed to call Groq API: " + e.getMessage(), e);
        }
    }

    private List<BusinessStrategyResponse> extractBusinessStrategies(GroqResponse response) {
        try {
            if (response == null || response.choices() == null || response.choices().isEmpty()) {
                throw new RuntimeException("Invalid response from Groq API");
            }

            String content = response.choices().get(0).message().content();
            
            // Parse JSON array response
            return objectMapper.readValue(content, new TypeReference<List<BusinessStrategyResponse>>() {});
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse business strategy response: " + e.getMessage(), e);
        }
    }

}
