package yenha.foodstore.Inventory.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yenha.foodstore.Inventory.Entity.DailyProductInventory;
import yenha.foodstore.Inventory.Repository.DailyProductInventoryRepository;
import yenha.foodstore.Menu.Entity.Product;
import yenha.foodstore.Menu.Repository.ProductRepository;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DailyInventoryScheduler {

    private final ProductRepository productRepository;
    private final DailyProductInventoryRepository inventoryRepository;

    /**
     * Create daily inventory records at midnight (00:00) every day
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void createDailyInventory() {
        LocalDate today = LocalDate.now();
        log.info("Starting daily inventory creation for {}", today);

        List<Product> activeProducts = productRepository.findByIsActiveTrue();
        int created = 0;
        int skipped = 0;

        for (Product product : activeProducts) {
            // Check if already exists (prevent duplicate)
            if (inventoryRepository.existsByProductAndDate(product, today)) {
                skipped++;
                continue;
            }

            // Validate product has required fields
            if (product.getCost() == null || product.getDefaultDailyLimit() == null) {
                log.warn("Product {} missing cost or dailyLimit, skipping", product.getProductId());
                skipped++;
                continue;
            }

            DailyProductInventory inventory = new DailyProductInventory();
            inventory.setProduct(product);
            inventory.setDate(today);
            
            // Snapshot from Product
            inventory.setPriceAtDate(product.getPrice());
            inventory.setCostAtDate(product.getCost());
            
            // Config (can be overridden later)
            inventory.setDailyLimit(product.getDefaultDailyLimit());
            inventory.setNumberRemain(product.getDefaultDailyLimit());
            
            inventoryRepository.save(inventory);
            created++;
        }

        log.info("Daily inventory creation completed: {} created, {} skipped, {} total active products", 
            created, skipped, activeProducts.size());
    }

    /**
     * Manual trigger for creating inventory (useful for testing or initial setup)
     */
    @Transactional
    public void createInventoryForDate(LocalDate date) {
        log.info("Manually creating inventory for {}", date);

        List<Product> activeProducts = productRepository.findByIsActiveTrue();
        int created = 0;

        for (Product product : activeProducts) {
            if (inventoryRepository.existsByProductAndDate(product, date)) {
                continue;
            }

            if (product.getCost() == null || product.getDefaultDailyLimit() == null) {
                log.warn("Product {} missing cost or dailyLimit, skipping", product.getProductId());
                continue;
            }

            DailyProductInventory inventory = new DailyProductInventory();
            inventory.setProduct(product);
            inventory.setDate(date);
            inventory.setPriceAtDate(product.getPrice());
            inventory.setCostAtDate(product.getCost());
            inventory.setDailyLimit(product.getDefaultDailyLimit());
            inventory.setNumberRemain(product.getDefaultDailyLimit());
            
            inventoryRepository.save(inventory);
            created++;
        }

        log.info("Manual inventory creation completed: {} records created for {}", created, date);
    }
}
