package com.telecom.subscription.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${server.port:8083}")
    private String serverPort;

    @Bean
    public OpenAPI subscriptionServiceOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Subscription Service API")
                .description("""
                    ## Subscription/Contract Management
                    
                    Manages customer subscriptions (contracts) linking customers to offers.
                    
                    ### Contract Lifecycle
                    - PENDING → ACTIVE → SUSPENDED → TERMINATED
                    
                    ### Key Features
                    - Create subscriptions linking customers to offers
                    - Activate, suspend, or terminate contracts
                    - Query subscriptions by customer or status
                    """)
                .version("1.0.0")
                .contact(new Contact().name("Telecom Platform Team").email("platform@telecom.com")))
            .servers(List.of(
                new Server().url("http://localhost:" + serverPort).description("Direct"),
                new Server().url("http://localhost:8080/api").description("API Gateway")
            ))
            .tags(List.of(
                new Tag().name("Subscriptions").description("Contract/subscription management")
            ));
    }
}
