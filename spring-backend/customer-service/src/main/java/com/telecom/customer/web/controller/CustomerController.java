package com.telecom.customer.web.controller;

import com.telecom.customer.application.CustomerService;
import com.telecom.customer.web.dto.ClientDto;
import com.telecom.customer.web.dto.CreateClientRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
public class CustomerController {
    
    private final CustomerService customerService;
    
    @GetMapping
    public ResponseEntity<List<ClientDto>> getAllCustomers() {
        return ResponseEntity.ok(customerService.getAllCustomers());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ClientDto> getCustomerById(@PathVariable Long id) {
        return ResponseEntity.ok(customerService.getCustomerById(id));
    }
    
    @GetMapping("/email/{email}")
    public ResponseEntity<ClientDto> getCustomerByEmail(@PathVariable String email) {
        return ResponseEntity.ok(customerService.getCustomerByEmail(email));
    }
    
    @PostMapping
    public ResponseEntity<ClientDto> createCustomer(@RequestBody CreateClientRequest request) {
        return ResponseEntity.ok(customerService.createCustomer(request));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ClientDto> updateCustomer(
            @PathVariable Long id,
            @RequestBody CreateClientRequest request) {
        return ResponseEntity.ok(customerService.updateCustomer(id, request));
    }
    
    @PostMapping("/{id}/suspend")
    public ResponseEntity<Void> suspendCustomer(@PathVariable Long id) {
        customerService.suspendCustomer(id);
        return ResponseEntity.ok().build();
    }
}
