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

    @Query("SELECT d FROM DailyProductInventory d JOIN FETCH d.product WHERE d.date = :date")
    List<DailyProductInventory> findAllByDate(@Param("date") LocalDate date);

    @Query("SELECT d FROM DailyProductInventory d JOIN FETCH d.product WHERE d.date >= :startDate AND d.date <= :endDate ORDER BY d.date DESC, d.product.productId ASC")
    List<DailyProductInventory> findByDateRange(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    @Query("SELECT d FROM DailyProductInventory d JOIN FETCH d.product WHERE d.product.productId = :productId AND d.date >= :startDate AND d.date <= :endDate ORDER BY d.date DESC")
    List<DailyProductInventory> findByProductIdAndDateRange(
        @Param("productId") Long productId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    @Query("SELECT d FROM DailyProductInventory d JOIN FETCH d.product WHERE d.date = :date AND d.numberRemain = 0")
    List<DailyProductInventory> findSoldOutProducts(@Param("date") LocalDate date);

    @Query("SELECT new yenha.foodstore.Inventory.DTO.InventoryAggregateDTO(" +
           "p.productId, " +
           "p.name, " +
           "c.name, " +
           "SUM((d.dailyLimit - d.numberRemain) * (d.priceAtDate - d.costAtDate)), " +
           "AVG(CASE WHEN d.dailyLimit = 0 THEN 0.0 ELSE CAST(d.dailyLimit - d.numberRemain AS double) / d.dailyLimit END), " +
           "CASE WHEN SUM(d.dailyLimit - d.numberRemain) = 0 THEN 0.0 ELSE SUM((d.dailyLimit - d.numberRemain) * (d.priceAtDate - d.costAtDate)) / SUM(d.dailyLimit - d.numberRemain) END) " +
           "FROM DailyProductInventory d " +
           "JOIN d.product p " +
           "JOIN p.category c " +
           "WHERE d.date BETWEEN :startDate AND :endDate " +
           "GROUP BY p.productId, p.name, c.name " +
           "ORDER BY SUM((d.dailyLimit - d.numberRemain) * (d.priceAtDate - d.costAtDate)) DESC")
    List<yenha.foodstore.Inventory.DTO.InventoryAggregateDTO> getAggregatedInventory(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
}
