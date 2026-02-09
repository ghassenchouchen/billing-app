package com.telecom.usage.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger configuration for Usage Service documentation
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8084}")
    private String serverPort;

    @Bean
    public OpenAPI usageServiceOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Usage Service API - Telecom Billing Platform")
                .description("""
                    ## Overview
                    The Usage Service handles CDR (Call Detail Record) ingestion and management 
                    for the telecom billing platform.
                    
                    ## Key Features
                    - **CDR Ingestion**: REST API for external systems to submit usage data
                    - **Idempotency**: Duplicate detection using session IDs
                    - **Audit Trail**: Raw CDR preservation for compliance
                    - **Event Publishing**: Kafka integration for downstream processing
                    
                    ## Usage Types
                    - `VOICE` - Voice calls (measured in seconds)
                    - `SMS` - Text messages (measured in count)
                    - `DATA` - Data usage (measured in bytes)
                    - `VOICE_ROAMING` / `DATA_ROAMING` / `SMS_ROAMING` - Roaming services
                    - `VAS` - Value-added services
                    
                    ## Integration
                    Usage records are linked to **Subscriptions** (not directly to Customers).
                    This allows proper billing hierarchy: Customer → Subscription → Usage.
                    
                    ## Idempotency
                    All CDR ingestion requests require a unique `sessionId`. 
                    Duplicate requests return HTTP 409 with the existing record.
                    """)
                .version("1.0.0")
                .contact(new Contact()
                    .name("Telecom Platform Team")
                    .email("platform@telecom.com"))
                .license(new License()
                    .name("Internal Use Only")))
            .servers(List.of(
                new Server()
                    .url("http://localhost:" + serverPort)
                    .description("Local Development Server"),
                new Server()
                    .url("http://localhost:8080/api")
                    .description("API Gateway")
            ))
            .tags(List.of(
                new Tag()
                    .name("CDR Ingestion")
                    .description("Endpoints for ingesting Call Detail Records from external systems"),
                new Tag()
                    .name("Usage Query")
                    .description("Endpoints for querying and reporting on usage data"),
                new Tag()
                    .name("Usage Management")
                    .description("Administrative endpoints for managing usage records")
            ));
    }
}
