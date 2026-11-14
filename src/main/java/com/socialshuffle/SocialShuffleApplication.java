package com.socialshuffle;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Main Spring Boot Application Entry Point for Social Shuffle.
 * Right-click in STS and select "Run As -> Spring Boot App".
 */
@SpringBootApplication
@EnableAsync
@EnableCaching
public class SocialShuffleApplication {

    public static void main(String[] args) {
        SpringApplication.run(SocialShuffleApplication.class, args);
        System.out.println("=================================================");
        System.out.println("Social Shuffle Pune Spring Boot API Started!");
        System.out.println("Server running at: http://localhost:8080/api");
        System.out.println("Database Connected (PostgreSQL / Supabase Cloud DB)");
        System.out.println("=================================================");
    }

    @Bean(name = "mailTaskExecutor")
    public Executor mailTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(12);
        executor.setQueueCapacity(250);
        executor.setThreadNamePrefix("MailAsync-");
        executor.initialize();
        return executor;
    }

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(
                "events",
                "games",
                "venues",
                "admins",
                "badgeLevels",
                "communityStats"
        );
    }
}
