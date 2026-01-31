package yenha.foodstore.Inventory.Controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import yenha.foodstore.Inventory.DTO.InventoryDTO;
import yenha.foodstore.Inventory.DTO.InventoryHistoryDTO;
import yenha.foodstore.Inventory.Service.DailyInventoryScheduler;
import yenha.foodstore.Inventory.Service.InventorySSEService;
import yenha.foodstore.Inventory.Service.InventoryService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;
    private final InventorySSEService sseService;
    private final DailyInventoryScheduler scheduler;

    /**
     * SSE endpoint - Client connects for real-time inventory updates
     * Fixed: Fetch data BEFORE creating SSE to avoid transaction leak
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamInventory() {
        // CRITICAL: Fetch data FIRST to ensure transaction completes
        // before SSE emitter is created and returned
        List<InventoryDTO> todayInventory = inventoryService.getTodayInventory();
        
        // Transaction is now closed, safe to create long-lived SSE connection
        SseEmitter emitter = sseService.createEmitter();
        sseService.sendInitialData(emitter, todayInventory);
        
        return emitter;
    }

    /**
     * Get today's inventory (fallback if SSE not working)
     */
    @GetMapping("/today")
    public ResponseEntity<List<InventoryDTO>> getTodayInventory() {
        List<InventoryDTO> inventory = inventoryService.getTodayInventory();
        return ResponseEntity.ok(inventory);
    }

    /**
     * Get inventory history for AI analysis
     * Example: GET /api/inventory/history?startDate=2025-01-01&endDate=2025-01-31
     */
    @GetMapping("/history")
    public ResponseEntity<List<InventoryHistoryDTO>> getInventoryHistory(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        List<InventoryHistoryDTO> history = inventoryService.getInventoryHistory(startDate, endDate);
        return ResponseEntity.ok(history);
    }

    /**
     * Get inventory history for specific product
     * Example: GET /api/inventory/history/1?startDate=2025-01-01&endDate=2025-01-31
     */
    @GetMapping("/history/{productId}")
    public ResponseEntity<List<InventoryHistoryDTO>> getProductInventoryHistory(
        @PathVariable Long productId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        List<InventoryHistoryDTO> history = 
            inventoryService.getProductInventoryHistory(productId, startDate, endDate);
        return ResponseEntity.ok(history);
    }

    /**
     * Get sold out products for today
     */
    @GetMapping("/sold-out")
    public ResponseEntity<List<InventoryDTO>> getSoldOutProducts() {
        List<InventoryDTO> soldOut = inventoryService.getSoldOutProducts();
        return ResponseEntity.ok(soldOut);
    }

    /**
     * Admin: Update daily limit for specific product and date
     * Example: PUT /api/inventory/1/limit?date=2025-01-31&limit=100
     */
    @PutMapping("/{productId}/limit")
    public ResponseEntity<?> updateDailyLimit(
        @PathVariable Long productId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @RequestParam Integer limit
    ) {
        try {
            inventoryService.updateDailyLimit(productId, date, limit);
            return ResponseEntity.ok(Map.of(
                "message", "Daily limit updated successfully",
                "productId", productId,
                "date", date.toString(),
                "newLimit", limit
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Admin: Manually trigger daily inventory creation
     * Example: POST /api/inventory/create?date=2025-01-31
     */
    @PostMapping("/create")
    public ResponseEntity<?> createInventoryManually(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        try {
            LocalDate targetDate = date != null ? date : LocalDate.now();
            scheduler.createInventoryForDate(targetDate);
            return ResponseEntity.ok(Map.of(
                "message", "Inventory created successfully",
                "date", targetDate.toString()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get SSE connection status
     */
    @GetMapping("/sse/status")
    public ResponseEntity<?> getSseStatus() {
        return ResponseEntity.ok(Map.of(
            "activeConnections", sseService.getActiveConnections()
        ));
    }

    /**
     * Test endpoint: Manually trigger SSE broadcast
     * Example: POST /api/inventory/test-broadcast?productId=1&numberRemain=95
     */
    @PostMapping("/test-broadcast")
    public ResponseEntity<?> testBroadcast(
        @RequestParam Long productId,
        @RequestParam Integer numberRemain
    ) {
        sseService.broadcastInventoryUpdate(productId, numberRemain);
        return ResponseEntity.ok(Map.of(
            "message", "Broadcast sent",
            "productId", productId,
            "numberRemain", numberRemain,
            "sentToClients", sseService.getActiveConnections()
        ));
    }
}
