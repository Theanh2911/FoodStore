package yenha.foodstore.Payment.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebhookResponseDTO {
    private boolean success;
    private String message;
    
    public static WebhookResponseDTO success() {
        return new WebhookResponseDTO(true, "OK");
    }
    
    public static WebhookResponseDTO error(String message) {
        return new WebhookResponseDTO(false, message);
    }
}

