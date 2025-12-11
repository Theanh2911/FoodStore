package yenha.foodstore.Payment.DTO;

import lombok.*;
import yenha.foodstore.Payment.Entity.Status;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankDTO {
    private Long id;
    private String bankName;
    private String accountNumber;
    private String accountHolder;
    private String qrCodeImageUrl;
    private Status status;
}

