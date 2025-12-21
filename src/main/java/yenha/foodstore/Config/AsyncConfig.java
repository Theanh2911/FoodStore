package yenha.foodstore.Config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {
    
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // Core pool size: số thread chạy thường xuyên
        executor.setCorePoolSize(5);
        
        // Max pool size: số thread tối đa
        executor.setMaxPoolSize(10);
        
        // Queue capacity: số task có thể chờ trong queue
        executor.setQueueCapacity(100);
        
        // Thread name prefix
        executor.setThreadNamePrefix("async-payment-");
        
        // Khởi tạo
        executor.initialize();
        
        return executor;
    }
}

