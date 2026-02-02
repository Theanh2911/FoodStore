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
    private Double amount;              // finalAmount (số tiền thực tế đã thanh toán)
    private Double totalAmount;         // Tổng tiền trước giảm giá
    private Double discountAmount;      // Số tiền được giảm
    private String promotionCode;       // Mã khuyến mãi đã dùng
    private String message;
    private String gateway;
    private String transactionDate;
    
    // Constructor cho thành công (legacy - giữ để backward compatible)
    public static PaymentEventDTO success(Long orderId, Long paymentId, Double amount, String gateway, String transactionDate) {
        return new PaymentEventDTO(
            orderId, 
            paymentId, 
            PaymentStatus.SUCCESS, 
            amount,
            null,  // totalAmount
            null,  // discountAmount
            null,  // promotionCode
            "Thanh toán thành công", 
            gateway, 
            transactionDate
        );
    }
    
    // Constructor mới với đầy đủ promotion info
    public static PaymentEventDTO successWithPromotion(
            Long orderId, 
            Long paymentId, 
            Double finalAmount,
            Double totalAmount,
            Double discountAmount,
            String promotionCode,
            String gateway, 
            String transactionDate) {
        return new PaymentEventDTO(
            orderId, 
            paymentId, 
            PaymentStatus.SUCCESS, 
            finalAmount,
            totalAmount,
            discountAmount,
            promotionCode,
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
            null,
            null,
            null,
            errorMessage, 
            null, 
            null
        );
    }
}

