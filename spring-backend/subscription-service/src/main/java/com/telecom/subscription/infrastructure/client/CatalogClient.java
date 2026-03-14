package com.telecom.subscription.infrastructure.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "catalog-service", url = "${services.catalog-url}")
public interface CatalogClient {
    
    @GetMapping("/offres/{id}")
    Map<String, Object> getOffreById(@PathVariable("id") Long id);
    
    @GetMapping("/services/{id}")
    Map<String, Object> getServiceById(@PathVariable("id") Long id);
}
