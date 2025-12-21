package yenha.foodstore.Payment.DTO;

import lombok.*;
import yenha.foodstore.Payment.Entity.Status;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankRequestDTO {
    private String bankName;
    private String accountNumber;
    private String accountHolder;
    private Status status;
}

