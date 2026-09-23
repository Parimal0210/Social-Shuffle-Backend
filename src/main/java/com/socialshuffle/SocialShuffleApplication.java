package com.socialshuffle;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * Main Spring Boot Application Entry Point for Social Shuffle.
 * Right-click in STS and select "Run As -> Spring Boot App".
 */
@SpringBootApplication
@EnableMongoRepositories(basePackages = "com.socialshuffle.repository")
public class SocialShuffleApplication {

    public static void main(String[] args) {
        SpringApplication.run(SocialShuffleApplication.class, args);
        System.out.println("=================================================");
        System.out.println("🎲 Social Shuffle Pune Spring Boot API Started!");
        System.out.println("📡 Server running at: http://localhost:8080/api");
        System.out.println("🍃 MongoDB Connected: social_shuffle database");
        System.out.println("=================================================");
    }
}
