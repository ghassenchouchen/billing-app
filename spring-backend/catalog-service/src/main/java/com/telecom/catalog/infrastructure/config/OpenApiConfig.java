package com.telecom.catalog.infrastructure.config;

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

    @Value("${server.port:8082}")
    private String serverPort;

    @Bean
    public OpenAPI catalogServiceOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Catalog Service API")
                .description("""
                    ## Product Catalog Management
                    
                    Manages telecom services and offers for the billing platform.
                    
                    ### Services
                    - VOICE - Voice call services
                    - DATA - Data/Internet services
                    - SMS - Text messaging services
                    - ROAMING - International roaming
                    - VALUE_ADDED - Premium services
                    
                    ### Offers
                    Bundled packages combining multiple services with pricing.
                    """)
                .version("1.0.0")
                .contact(new Contact()
                    .name("Telecom Platform Team")
                    .email("platform@telecom.com")))
            .servers(List.of(
                new Server().url("http://localhost:" + serverPort).description("Direct"),
                new Server().url("http://localhost:8080/api").description("API Gateway")
            ))
            .tags(List.of(
                new Tag().name("Services").description("Telecom service management"),
                new Tag().name("Offers").description("Product offer management")
            ));
    }
}
