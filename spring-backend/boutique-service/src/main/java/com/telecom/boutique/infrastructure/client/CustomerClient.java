package com.telecom.boutique.infrastructure.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
@Slf4j
public class CustomerClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String customerServiceUrl;

    public CustomerClient(@Value("${customer.service.url:http://localhost:8081}") String customerServiceUrl) {
        this.customerServiceUrl = customerServiceUrl;
    }

    public boolean customerExists(Long customerId) {
        String url = customerServiceUrl + "/customers/" + customerId;
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, null, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (RestClientException ex) {
            log.warn("Customer lookup failed for id={} at {}: {}", customerId, url, ex.getMessage());
            return false;
        }
    }
}
