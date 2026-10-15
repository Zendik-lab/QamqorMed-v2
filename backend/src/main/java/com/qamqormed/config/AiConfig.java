package com.qamqormed.config;

import com.qamqormed.ai.OllamaClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class AiConfig {

    @Bean(name = "aiObjectMapper")
    com.fasterxml.jackson.databind.ObjectMapper aiObjectMapper() {
        return new com.fasterxml.jackson.databind.ObjectMapper();
    }

    @Bean(destroyMethod = "shutdown")
    ExecutorService aiExecutor(AiProperties properties) {
        int poolSize = Math.max(2, Math.min(8, properties.getMaxConcurrentRequests() + 1));
        return Executors.newFixedThreadPool(poolSize, runnable -> {
            Thread thread = new Thread(runnable, "qamqormed-ai");
            thread.setDaemon(true);
            return thread;
        });
    }

    @Bean
    OllamaClient ollamaClient(AiProperties properties,
                              com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        return new OllamaClient(properties, objectMapper);
    }
}
