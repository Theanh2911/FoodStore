package yenha.foodstore.Payment.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SepayWebhookDTO {
    
    @JsonProperty("id")
    private Long id;
    
    @JsonProperty("gateway")
    private String gateway;
    
    @JsonProperty("transactionDate")
    private String transactionDate;
    
    @JsonProperty("accountNumber")
    private String accountNumber;
    
    @JsonProperty("code")
    private String code;
    
    @JsonProperty("content")
    private String content;
    
    @JsonProperty("transferType")
    private String transferType;
    
    @JsonProperty("transferAmount")
    private Double transferAmount;
    
    @JsonProperty("accumulated")
    private Double accumulated;
    
    @JsonProperty("subAccount")
    private String subAccount;
    
    @JsonProperty("referenceCode")
    private String referenceCode;
    
    @JsonProperty("description")
    private String description;
}

