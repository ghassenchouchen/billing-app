package com.telecom.customer.web.controller;

import com.telecom.customer.application.CustomerService;
import com.telecom.customer.web.dto.ClientDto;
import com.telecom.customer.web.dto.CreateClientRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * CUSTOMER CONTROLLER - REST API for customer management
 * 
 * ENDPOINTS:
 * - Customer CRUD operations
 * - Customer lookup by id, customerRef, email
 * - Account balance operations
 * - Customer lifecycle management (suspend, reactivate)
 */
@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
public class CustomerController {
    
    private final CustomerService customerService;
    
    
    @GetMapping
    public ResponseEntity<List<ClientDto>> getAllCustomers() {
        return ResponseEntity.ok(customerService.getAllCustomers());
    }
    
    @GetMapping("/active")
    public ResponseEntity<List<ClientDto>> getAllActiveCustomers() {
        return ResponseEntity.ok(customerService.getAllActiveCustomers());
    }
    
    // Will be removed in future version
    @Deprecated
    @GetMapping("/{id}")
    public ResponseEntity<ClientDto> getCustomerById(@PathVariable Long id) {
        return ResponseEntity.ok(customerService.getCustomerById(id));
    }
    
    // PRIMARY ENDPOINT - Use this for customer lookup
    @GetMapping("/ref/{customerRef}")
    public ResponseEntity<ClientDto> getCustomerByRef(@PathVariable String customerRef) {
        return ResponseEntity.ok(customerService.getCustomerByRef(customerRef));
    }
    
    @GetMapping("/email/{email}")
    public ResponseEntity<ClientDto> getCustomerByEmail(@PathVariable String email) {
        return ResponseEntity.ok(customerService.getCustomerByEmail(email));
    }
    
    // === COMMAND ENDPOINTS ===
    
    @PostMapping
    public ResponseEntity<ClientDto> createCustomer(@RequestBody CreateClientRequest request) {
        ClientDto customer = customerService.createCustomer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(customer);
    }
    
    // INTERNAL USE ONLY - Will be removed
    @Deprecated
    @PutMapping("/{id}")
    public ResponseEntity<ClientDto> updateCustomer(
            @PathVariable Long id,
            @RequestBody CreateClientRequest request) {
        return ResponseEntity.ok(customerService.updateCustomer(id, request));
    }
    
    // PRIMARY ENDPOINT - Use customerRef
    @PutMapping("/ref/{customerRef}")
    public ResponseEntity<ClientDto> updateCustomerByRef(
            @PathVariable String customerRef,
            @RequestBody CreateClientRequest request) {
        return ResponseEntity.ok(customerService.updateCustomerByRef(customerRef, request));
    }
    
    // INTERNAL USE ONLY - Will be removed
    @Deprecated
    @PostMapping("/{id}/suspend")
    public ResponseEntity<Void> suspendCustomer(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "Manual suspension") String reason) {
        customerService.suspendCustomer(id, reason);
        return ResponseEntity.ok().build();
    }
    
    // PRIMARY ENDPOINT - Use customerRef for suspension
    @PostMapping("/ref/{customerRef}/suspend")
    public ResponseEntity<Void> suspendCustomerByRef(
            @PathVariable String customerRef,
            @RequestParam(required = false, defaultValue = "Manual suspension") String reason) {
        customerService.suspendCustomerByRef(customerRef, reason);
        return ResponseEntity.ok().build();
    }
    
    @Deprecated
    @PostMapping("/{id}/reactivate")
    public ResponseEntity<ClientDto> reactivateCustomer(@PathVariable Long id) {
        return ResponseEntity.ok(customerService.reactivateCustomer(id));
    }
    
    // PRIMARY ENDPOINT - Use customerRef for reactivation
    @PostMapping("/ref/{customerRef}/reactivate")
    public ResponseEntity<ClientDto> reactivateCustomerByRef(@PathVariable String customerRef) {
        return ResponseEntity.ok(customerService.reactivateCustomerByRef(customerRef));
    }
    
    
    @GetMapping("/ref/{customerRef}/balance")
    public ResponseEntity<Map<String, Object>> getBalance(@PathVariable String customerRef) {
        BigDecimal balance = customerService.getBalance(customerRef);
        return ResponseEntity.ok(Map.of(
            "customerRef", customerRef,
            "balance", balance
        ));
    }
    
    @PostMapping("/ref/{customerRef}/credit")
    public ResponseEntity<ClientDto> addCredit(
            @PathVariable String customerRef,
            @RequestParam BigDecimal amount,
            @RequestParam(required = false, defaultValue = "Manual credit") String description) {
        return ResponseEntity.ok(customerService.addCredit(customerRef, amount, description));
    }
    
    @PostMapping("/ref/{customerRef}/debit")
    public ResponseEntity<ClientDto> deductCredit(
            @PathVariable String customerRef,
            @RequestParam BigDecimal amount,
            @RequestParam(required = false, defaultValue = "Manual debit") String description) {
        return ResponseEntity.ok(customerService.deductCredit(customerRef, amount, description));
    }
    
    @PutMapping("/ref/{customerRef}/credit-limit")
    public ResponseEntity<ClientDto> updateCreditLimit(
            @PathVariable String customerRef,
            @RequestParam BigDecimal creditLimit) {
        return ResponseEntity.ok(customerService.updateCreditLimit(customerRef, creditLimit));
    }
    
    @GetMapping("/ref/{customerRef}/can-charge")
    public ResponseEntity<Map<String, Object>> canCharge(
            @PathVariable String customerRef,
            @RequestParam BigDecimal amount) {
        boolean canCharge = customerService.canCharge(customerRef, amount);
        return ResponseEntity.ok(Map.of(
            "customerRef", customerRef,
            "amount", amount,
            "canCharge", canCharge
        ));
    }
}
