package yenha.foodstore.Payment.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import yenha.foodstore.Payment.Entity.Bank;
import yenha.foodstore.Payment.Entity.Status;

import java.util.List;
import java.util.Optional;

@Repository
public interface BankRepository extends JpaRepository<Bank, Long> {
    List<Bank> findByStatus(Status status);
    Optional<Bank> findByAccountNumber(String accountNumber);
}
