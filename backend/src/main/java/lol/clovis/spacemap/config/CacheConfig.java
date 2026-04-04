package lol.clovis.spacemap.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCache tleCache = new CaffeineCache("tle",
                Caffeine.newBuilder()
                        .expireAfterWrite(30, TimeUnit.MINUTES)
                        .maximumSize(50)
                        .recordStats()
                        .build());
        CaffeineCache planetCache = new CaffeineCache("planets",
                Caffeine.newBuilder()
                        .expireAfterWrite(24, TimeUnit.HOURS)
                        .maximumSize(1)
                        .recordStats()
                        .build());
        CaffeineCache spacecraftCache = new CaffeineCache("spacecraft",
                Caffeine.newBuilder()
                        .expireAfterWrite(6, TimeUnit.HOURS)
                        .maximumSize(1)
                        .recordStats()
                        .build());
        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(List.of(tleCache, planetCache, spacecraftCache));
        return manager;
    }
}
