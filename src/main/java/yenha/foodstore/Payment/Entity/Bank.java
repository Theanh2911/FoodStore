package yenha.foodstore.Payment.Entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "banking_info")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bank {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String bankName;
    private String accountNumber;
    private String accountHolder;
    private String qrCodeImageUrl;
    
    @Enumerated(EnumType.STRING)
    private Status status;
}
