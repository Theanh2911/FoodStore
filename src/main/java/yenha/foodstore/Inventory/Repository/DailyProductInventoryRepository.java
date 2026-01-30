package yenha.foodstore.Inventory.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import yenha.foodstore.Inventory.Entity.DailyProductInventory;
import yenha.foodstore.Menu.Entity.Product;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyProductInventoryRepository extends JpaRepository<DailyProductInventory, Long> {

    Optional<DailyProductInventory> findByProductProductIdAndDate(Long productId, LocalDate date);

    boolean existsByProductAndDate(Product product, LocalDate date);

    List<DailyProductInventory> findAllByDate(LocalDate date);

    @Query("SELECT d FROM DailyProductInventory d WHERE d.date >= :startDate AND d.date <= :endDate ORDER BY d.date DESC, d.product.productId ASC")
    List<DailyProductInventory> findByDateRange(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    @Query("SELECT d FROM DailyProductInventory d WHERE d.product.productId = :productId AND d.date >= :startDate AND d.date <= :endDate ORDER BY d.date DESC")
    List<DailyProductInventory> findByProductIdAndDateRange(
        @Param("productId") Long productId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    @Query("SELECT d FROM DailyProductInventory d WHERE d.date = :date AND d.numberRemain = 0")
    List<DailyProductInventory> findSoldOutProducts(@Param("date") LocalDate date);
}
