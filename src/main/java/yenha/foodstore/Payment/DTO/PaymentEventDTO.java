package yenha.foodstore.Payment.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import yenha.foodstore.Payment.Entity.PaymentStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEventDTO {
    private Long orderId;
    private Long paymentId;
    private PaymentStatus status;
    private Double amount;
    private String message;
    private String gateway;
    private String transactionDate;
    
    // Constructor cho thành công
    public static PaymentEventDTO success(Long orderId, Long paymentId, Double amount, String gateway, String transactionDate) {
        return new PaymentEventDTO(
            orderId, 
            paymentId, 
            PaymentStatus.SUCCESS, 
            amount, 
            "Thanh toán thành công", 
            gateway, 
            transactionDate
        );
    }
    
    // Constructor cho thất bại
    public static PaymentEventDTO failed(Long orderId, String errorMessage) {
        return new PaymentEventDTO(
            orderId, 
            null, 
            PaymentStatus.FAILED, 
            null, 
            errorMessage, 
            null, 
            null
        );
    }
}

