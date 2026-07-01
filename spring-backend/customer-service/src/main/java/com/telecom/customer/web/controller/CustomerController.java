package com.telecom.customer.web.controller;

import com.telecom.customer.application.CustomerService;
import com.telecom.customer.web.dto.ClientDto;
import com.telecom.customer.web.dto.CreateClientRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * CUSTOMER CONTROLLER - REST API for customer management
 
 * ENDPOINTS:
  customer CRUD operations
  customer lookup by  customerRef, boutiqueRef, email, cin/passport
  account balance operations
  customer lifecycle management (suspend, reactivate)
 */
@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
@Slf4j
public class CustomerController {
    
    private final CustomerService customerService;
    
    
    @GetMapping
    public ResponseEntity<List<ClientDto>> getAllCustomers(
        @RequestParam(required = false) String boutiqueRef,
        @RequestHeader(value = "X-Auth-Role", required = false) String role,
        @RequestHeader(value = "X-Auth-Boutique-Id", required = false) String authBoutiqueId
    ) {
        log.info(" Demo PFE - (Live Pipeline)");
        // Enforce boutique-scoped access for non-ADMIN users
        if (!"ADMIN".equals(role) && authBoutiqueId != null) {
            boutiqueRef = authBoutiqueId; // Force assigned boutique
        }
        
        if (boutiqueRef != null) {
            return ResponseEntity.ok(customerService.getCustomersByBoutique(boutiqueRef));
        }
        return ResponseEntity.ok(customerService.getAllCustomers());
    }

    @GetMapping("/paged")
    public ResponseEntity<Page<ClientDto>> getAllCustomersPaged(
        @PageableDefault(size = 20, sort = "id") Pageable pageable
    ) {
        return ResponseEntity.ok(customerService.getAllCustomersPaged(pageable));
    }
    
    @GetMapping("/active")
    public ResponseEntity<List<ClientDto>> getAllActiveCustomers(
        @RequestHeader(value = "X-Auth-Role", required = false) String role,
        @RequestHeader(value = "X-Auth-Boutique-Id", required = false) String authBoutiqueId
    ) {
        // Enforce boutique-scoped access
        if (!"ADMIN".equals(role) && authBoutiqueId != null) {
            return ResponseEntity.ok(customerService.getActiveCustomersByBoutique(authBoutiqueId));
        }
        return ResponseEntity.ok(customerService.getAllActiveCustomers());
    }
    
    // Will be removed in future version
    @Deprecated
    @GetMapping("/{id}")
    public ResponseEntity<ClientDto> getCustomerById(@PathVariable Long id) {
        return ResponseEntity.ok(customerService.getCustomerById(id));
    }
    
    @GetMapping("/ref/{customerRef}")
    public ResponseEntity<ClientDto> getCustomerByRef(
        @PathVariable String customerRef,
        @RequestHeader(value = "X-Auth-Role", required = false) String role,
        @RequestHeader(value = "X-Auth-Boutique-Id", required = false) String authBoutiqueId
    ) {
        ClientDto customer = customerService.getCustomerByRef(customerRef);
        
        // Authorization check: boutique-scoped users can only access their own boutique customers
        if (!"ADMIN".equals(role) && authBoutiqueId != null) {
            if (!authBoutiqueId.equals(customer.boutiqueRef())) {
                log.warn("Authorization failed: User with boutiqueId={} attempted to access customer {} with boutiqueRef={}", 
                    authBoutiqueId, customerRef, customer.boutiqueRef());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            log.debug("Customer access granted: ref={}, role={}, authBoutiqueId={}, customerBoutiqueRef={}", 
                customerRef, role, authBoutiqueId, customer.boutiqueRef());
        }
        
        return ResponseEntity.ok(customer);
    }
    
    @GetMapping("/email/{email}")
    public ResponseEntity<ClientDto> getCustomerByEmail(@PathVariable String email) {
        return ResponseEntity.ok(customerService.getCustomerByEmail(email));
    }
    @GetMapping("/pieceIdentite/{pieceIdentite}")
    public ResponseEntity<ClientDto> getCustomerBypieceIdentite(@PathVariable String pieceIdentite ){
        return ResponseEntity.ok(customerService.getCustomerBypieceIdentite(pieceIdentite));
        
    }
    
    @PostMapping
    public ResponseEntity<ClientDto> createCustomer(
        @Valid @RequestBody CreateClientRequest request,
        @RequestHeader(value = "X-Auth-Role", required = false) String role,
        @RequestHeader(value = "X-Auth-Boutique-Id", required = false) String authBoutiqueId
    ) {
        // Force boutiqueRef from auth context for boutique-scoped roles
        ClientDto customer = customerService.createCustomer(request, role, authBoutiqueId);
        return ResponseEntity.status(HttpStatus.CREATED).body(customer);
    }
    
    @Deprecated
    @PutMapping("/{id}")
    public ResponseEntity<ClientDto> updateCustomer(
            @PathVariable Long id,
            @RequestBody CreateClientRequest request) {
        return ResponseEntity.ok(customerService.updateCustomer(id, request));
    }
    
    @PutMapping("/ref/{customerRef}")
    public ResponseEntity<ClientDto> updateCustomerByRef(
            @PathVariable String customerRef,
            @RequestBody CreateClientRequest request,
            @RequestHeader(value = "X-Auth-Role", required = false) String role,
            @RequestHeader(value = "X-Auth-Boutique-Id", required = false) String authBoutiqueId
    ) {
        // Check authorization before update
        ClientDto existing = customerService.getCustomerByRef(customerRef);
        if (!"ADMIN".equals(role) && authBoutiqueId != null) {
            if (!authBoutiqueId.equals(existing.boutiqueRef())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }
        
        return ResponseEntity.ok(customerService.updateCustomerByRef(customerRef, request));
    }
    
    @Deprecated
    @PostMapping("/{id}/suspend")
    public ResponseEntity<Void> suspendCustomer(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "Manual suspension") String reason) {
        customerService.suspendCustomer(id, reason);
        return ResponseEntity.ok().build();
    }
    
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
