package yenha.foodstore.Config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("tokenBlacklist") {
            @Override
            protected org.springframework.cache.Cache createConcurrentMapCache(String name) {
                return new ConcurrentMapCache(
                    name,
                    new java.util.concurrent.ConcurrentHashMap<>(1000),
                    false
                );
            }
        };
    }
}
