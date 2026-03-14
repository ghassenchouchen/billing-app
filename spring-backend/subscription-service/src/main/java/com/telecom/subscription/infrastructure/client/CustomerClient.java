package com.telecom.subscription.infrastructure.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "customer-service", url = "${services.customer-url}")
public interface CustomerClient {
    
    @GetMapping("/customers/{id}")
    Map<String, Object> getCustomerById(@PathVariable("id") Long id);

    @GetMapping("/customers/ref/{customerRef}")
    Map<String, Object> getCustomerByRef(@PathVariable("customerRef") String customerRef);
}
