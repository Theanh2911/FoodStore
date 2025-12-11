package yenha.foodstore.Payment.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QRCodeResponseDTO {
    private String qrCodeUrl;
    private Double amount;
    private String addInfo;
    private String bankName;
    private String accountNumber;
    private String accountHolder;
}



