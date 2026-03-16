package com.telecom.billing.infrastructure.config;

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

    @Value("${server.port:8085}")
    private String serverPort;

    @Bean
    public OpenAPI billingServiceOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Billing Service API")
                .description("""
                    ## Invoice Generation & Management
                    
                    Generates and manages invoices based on subscriptions and usage.
                    
                    ### Invoice Lifecycle
                    - DRAFT → FINALIZED → SENT → PAID/OVERDUE/CANCELLED
                    
                    ### Key Features
                    - Generate invoices for subscription periods
                    - Include subscription fees and usage charges
                    - Calculate taxes (TVA)
                    - Track payment status
                    """)
                .version("1.0.0")
                .contact(new Contact().name("Telecom Platform Team").email("platform@telecom.com")))
            .servers(List.of(
                new Server().url("http://localhost:" + serverPort).description("Direct"),
                new Server().url("http://localhost:8080/api").description("API Gateway")
            ))
            .tags(List.of(
                new Tag().name("Invoices").description("Invoice management operations")
            ));
    }
}
