package com.telecom.boutique.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI boutiqueServiceOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Boutique Service API")
                .description("RESTful API for Boutique (retail store) management — Telecom BSS")
                .version("1.0.0")
                .contact(new Contact()
                    .name("Telecom Billing Team")
                    .email("billing@telecom.com")))
            .servers(List.of(
                new Server().url("http://localhost:8088").description("Local Development"),
                new Server().url("http://localhost:8090").description("API Gateway")
            ));
    }
}
