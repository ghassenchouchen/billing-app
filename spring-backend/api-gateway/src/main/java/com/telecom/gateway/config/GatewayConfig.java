package com.telecom.gateway.config;

import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import reactor.core.publisher.Mono;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Configuration
@Slf4j
public class GatewayConfig {

    @Bean
    @Order(-1)
    public GlobalFilter correlationIdFilter() {
        return (exchange, chain) -> {
            String correlationId = exchange.getRequest()
                .getHeaders()
                .getFirst("X-Correlation-ID");
            
            if (correlationId == null) {
                correlationId = UUID.randomUUID().toString();
            }
            
            final String finalCorrelationId = correlationId;
            
            log.debug("Processing request with correlation ID: {}", finalCorrelationId);
            
            return chain.filter(
                exchange.mutate()
                    .request(exchange.getRequest().mutate()
                        .header("X-Correlation-ID", finalCorrelationId)
                        .build())
                    .build()
            ).then(Mono.fromRunnable(() -> {
                exchange.getResponse().getHeaders()
                    .add("X-Correlation-ID", finalCorrelationId);
            }));
        };
    }
    
    @Bean
    @Order(0)
    public GlobalFilter loggingFilter() {
        return (exchange, chain) -> {
            log.info("Request: {} {}", 
                exchange.getRequest().getMethod(),
                exchange.getRequest().getURI());
            
            long startTime = System.currentTimeMillis();
            
            return chain.filter(exchange).then(Mono.fromRunnable(() -> {
                long duration = System.currentTimeMillis() - startTime;
                log.info("Response: {} - {} ms", 
                    exchange.getResponse().getStatusCode(),
                    duration);
            }));
        };
    }
}
