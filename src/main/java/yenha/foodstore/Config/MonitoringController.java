package yenha.foodstore.Config;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import yenha.foodstore.Inventory.Service.InventorySSEService;
import yenha.foodstore.Payment.Service.SSEService;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/monitoring")
@RequiredArgsConstructor
public class MonitoringController {

    private final DataSource dataSource;
    private final InventorySSEService inventorySSEService;
    private final SSEService paymentSSEService;

    @GetMapping("/connection-pool")
    public ResponseEntity<?> getConnectionPoolStats() {
        if (dataSource instanceof HikariDataSource) {
            HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
            HikariPoolMXBean poolMXBean = hikariDataSource.getHikariPoolMXBean();

            Map<String, Object> stats = new HashMap<>();
            stats.put("activeConnections", poolMXBean.getActiveConnections());
            stats.put("idleConnections", poolMXBean.getIdleConnections());
            stats.put("totalConnections", poolMXBean.getTotalConnections());
            stats.put("threadsAwaitingConnection", poolMXBean.getThreadsAwaitingConnection());
            stats.put("maximumPoolSize", hikariDataSource.getMaximumPoolSize());
            stats.put("minimumIdle", hikariDataSource.getMinimumIdle());
            
            // Add SSE connection counts
            stats.put("inventorySSEConnections", inventorySSEService.getActiveConnections());
            
            // Warning if connections are exhausted
            if (poolMXBean.getThreadsAwaitingConnection() > 0) {
                stats.put("warning", "Threads waiting for connections! Possible connection leak.");
            }

            return ResponseEntity.ok(stats);
        }
        
        return ResponseEntity.ok(Map.of("error", "DataSource is not HikariCP"));
    }
    
    @PostMapping("/soft-evict-connections")
    public ResponseEntity<?> softEvictConnections() {
        if (dataSource instanceof HikariDataSource) {
            HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
            HikariPoolMXBean poolMXBean = hikariDataSource.getHikariPoolMXBean();
            
            // Soft evict idle connections to refresh the pool
            poolMXBean.softEvictConnections();
            
            return ResponseEntity.ok(Map.of(
                "message", "Soft eviction triggered",
                "activeConnections", poolMXBean.getActiveConnections(),
                "idleConnections", poolMXBean.getIdleConnections()
            ));
        }
        
        return ResponseEntity.badRequest().body(Map.of("error", "DataSource is not HikariCP"));
    }
}
