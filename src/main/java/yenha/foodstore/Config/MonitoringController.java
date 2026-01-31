package yenha.foodstore.Config;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/monitoring")
@RequiredArgsConstructor
public class MonitoringController {

    private final DataSource dataSource;

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

            return ResponseEntity.ok(stats);
        }
        
        return ResponseEntity.ok(Map.of("error", "DataSource is not HikariCP"));
    }
}
