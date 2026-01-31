package yenha.foodstore.Inventory.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import yenha.foodstore.Inventory.DTO.InventoryDTO;
import yenha.foodstore.Inventory.DTO.InventoryHistoryDTO;
import yenha.foodstore.Inventory.Entity.DailyProductInventory;
import yenha.foodstore.Inventory.Exception.InsufficientInventoryException;
import yenha.foodstore.Inventory.Exception.InventoryNotFoundException;
import yenha.foodstore.Inventory.Repository.DailyProductInventoryRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final DailyProductInventoryRepository inventoryRepository;
    private final InventorySSEService inventorySSEService;

    /**
     * Decrease inventory for a product (called from OrderService)
     * EVENT-DRIVEN: Broadcasts update to all SSE clients after save
     */
    @Transactional
    public DailyProductInventory decreaseInventory(Long productId, Integer quantity, LocalDate date) {
        DailyProductInventory inventory = inventoryRepository
            .findByProductProductIdAndDate(productId, date)
            .orElseThrow(() -> new InventoryNotFoundException(
                "Inventory not found for product " + productId + " on " + date
            ));

        if (inventory.getNumberRemain() < quantity) {
            throw new InsufficientInventoryException(
                "Product " + inventory.getProduct().getName() + 
                " only has " + inventory.getNumberRemain() + " items remaining today"
            );
        }

        Integer oldQuantity = inventory.getNumberRemain();
        inventory.setNumberRemain(oldQuantity - quantity);
        
        DailyProductInventory saved = inventoryRepository.save(inventory);
        
        log.info("Decreased inventory for product {}: {} -> {}", 
            productId, oldQuantity, saved.getNumberRemain());
        
        // EVENT-DRIVEN: Broadcast to ALL SSE clients (1 query -> N clients)
        inventorySSEService.broadcastInventoryUpdate(productId, saved.getNumberRemain());
        log.debug("Broadcasted inventory update to {} SSE clients", 
                inventorySSEService.getActiveConnections());
        
        return saved;
    }

    /**
     * Get today's inventory for all products
     */
    @Transactional(readOnly = true)
    public List<InventoryDTO> getTodayInventory() {
        LocalDate today = LocalDate.now();
        List<DailyProductInventory> inventories = inventoryRepository.findAllByDate(today);
        
        return inventories.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    /**
     * Get today's inventory for SSE - NO @Transactional annotation
     * Spring Data JPA will auto-create and immediately close transaction per query
     * This prevents transaction leak into SSE connection
     */
    public List<InventoryDTO> getTodayInventoryForSSE() {
        LocalDate today = LocalDate.now();
        // Repository method will open & close its own transaction automatically
        List<DailyProductInventory> inventories = inventoryRepository.findAllByDate(today);
        
        // Convert to DTO (no database access here, so no transaction needed)
        List<InventoryDTO> result = inventories.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
        
        log.debug("Fetched {} inventory items for SSE (no transaction context)", result.size());
        return result;
    }

    /**
     * Get inventory history for AI analysis
     */
    @Transactional(readOnly = true)
    public List<InventoryHistoryDTO> getInventoryHistory(LocalDate startDate, LocalDate endDate) {
        List<DailyProductInventory> inventories = 
            inventoryRepository.findByDateRange(startDate, endDate);
        
        return inventories.stream()
            .map(this::convertToHistoryDTO)
            .collect(Collectors.toList());
    }

    /**
     * Get inventory history for specific product
     */
    @Transactional(readOnly = true)
    public List<InventoryHistoryDTO> getProductInventoryHistory(
        Long productId, LocalDate startDate, LocalDate endDate
    ) {
        List<DailyProductInventory> inventories = 
            inventoryRepository.findByProductIdAndDateRange(productId, startDate, endDate);
        
        return inventories.stream()
            .map(this::convertToHistoryDTO)
            .collect(Collectors.toList());
    }

    /**
     * Get sold out products for today
     */
    @Transactional(readOnly = true)
    public List<InventoryDTO> getSoldOutProducts() {
        LocalDate today = LocalDate.now();
        List<DailyProductInventory> soldOut = inventoryRepository.findSoldOutProducts(today);
        
        return soldOut.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    /**
     * Update daily limit for specific product and date (admin function)
     */
    @Transactional
    public DailyProductInventory updateDailyLimit(Long productId, LocalDate date, Integer newLimit) {
        DailyProductInventory inventory = inventoryRepository
            .findByProductProductIdAndDate(productId, date)
            .orElseThrow(() -> new InventoryNotFoundException(
                "Inventory not found for product " + productId + " on " + date
            ));

        Integer soldQuantity = inventory.getSoldQuantity();
        inventory.setDailyLimit(newLimit);
        inventory.setNumberRemain(newLimit - soldQuantity);
        
        if (inventory.getNumberRemain() < 0) {
            inventory.setNumberRemain(0);
        }
        
        return inventoryRepository.save(inventory);
    }

    // Helper methods
    private InventoryDTO convertToDTO(DailyProductInventory inventory) {
        return new InventoryDTO(
            inventory.getProduct().getProductId(),
            inventory.getProduct().getName(),
            inventory.getNumberRemain(),
            inventory.getDailyLimit(),
            inventory.getPriceAtDate(),
            inventory.getCostAtDate()
        );
    }

    private InventoryHistoryDTO convertToHistoryDTO(DailyProductInventory inventory) {
        return new InventoryHistoryDTO(
            inventory.getProduct().getProductId(),
            inventory.getProduct().getName(),
            inventory.getDate(),
            inventory.getDailyLimit(),
            inventory.getNumberRemain(),
            inventory.getSoldQuantity(),
            inventory.getPriceAtDate(),
            inventory.getCostAtDate(),
            inventory.getProfitPerUnit(),
            inventory.getTotalRevenue(),
            inventory.getTotalCost(),
            inventory.getTotalProfit()
        );
    }
}
