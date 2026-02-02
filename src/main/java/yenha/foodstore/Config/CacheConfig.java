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
        return new ConcurrentMapCacheManager("tokenBlacklist", "activeProducts") {
            @Override
            protected org.springframework.cache.Cache createConcurrentMapCache(String name) {
                // tokenBlacklist: ~1000 tokens
                // activeProducts: ~100 products  
                int initialCapacity = name.equals("tokenBlacklist") ? 1000 : 100;
                return new ConcurrentMapCache(
                    name,
                    new java.util.concurrent.ConcurrentHashMap<>(initialCapacity),
                    false
                );
            }
        };
    }
}
