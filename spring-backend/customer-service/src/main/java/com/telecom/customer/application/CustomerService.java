package com.telecom.customer.application;

import com.telecom.customer.domain.entity.Customer;
import com.telecom.customer.domain.repository.CustomerRepository;
import com.telecom.customer.infrastructure.kafka.CustomerEventPublisher;
import com.telecom.customer.web.dto.ClientDto;
import com.telecom.customer.web.dto.CreateClientRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Year;
import java.util.List;
import java.util.UUID;


@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerService {
    
    private final CustomerRepository customerRepository;
    private final CustomerEventPublisher eventPublisher;
    
    
    @Transactional(readOnly = true)
    public List<ClientDto> getAllCustomers() {
        return customerRepository.findAll().stream()
            .map(this::toDto)
            .toList();
    }
    
    @Transactional(readOnly = true)
    public List<ClientDto> getAllActiveCustomers() {
        return customerRepository.findAllActive().stream()
            .map(this::toDto)
            .toList();
    }
    
    @Transactional(readOnly = true)
    public ClientDto getCustomerById(Long id) {
        return customerRepository.findById(id)
            .map(this::toDto)
            .orElseThrow(() -> new RuntimeException("Customer not found: " + id));
    }
    
    @Transactional(readOnly = true)
    public ClientDto getCustomerByRef(String customerRef) {
        return customerRepository.findByCustomerRef(customerRef)
            .map(this::toDto)
            .orElseThrow(() -> new RuntimeException("Customer not found: " + customerRef));
    }
    
    @Transactional(readOnly = true)
    public ClientDto getCustomerByEmail(String email) {
        return customerRepository.findByEmail(email)
            .map(this::toDto)
            .orElseThrow(() -> new RuntimeException("Customer not found: " + email));
    }
    @Transactional(readOnly = true)
    public ClientDto getCustomerBypieceIdentite(String pieceIdentite) {
        return customerRepository.findByPieceIdentite(pieceIdentite)
            .map(this::toDto)
            .orElseThrow(() -> new RuntimeException("Customer not found: " + pieceIdentite));
    }
    @Transactional(readOnly = true)
    public List<ClientDto> getCustomersByBoutique(String boutiqueRef) {
        return customerRepository.findByBoutiqueRef(boutiqueRef).stream()
            .map(this::toDto)
            .toList();
    }
    
    @Transactional(readOnly = true)
    public List<ClientDto> getActiveCustomersByBoutique(String boutiqueRef) {
        return customerRepository.findByBoutiqueRef(boutiqueRef).stream()
            .filter(c -> c.getStatus() == Customer.ClientStatus.ACTIVE)
            .map(this::toDto)
            .toList();
    }

    
    @Transactional
    public ClientDto createCustomer(CreateClientRequest request, String role, String authBoutiqueId) {
        if (customerRepository.existsByEmail(request.email())) {
            throw new RuntimeException("Email already exists: " + request.email());
        }
        
        // Determine boutiqueRef: force from auth context if caller is boutique-scoped
        String boutiqueRef = request.boutiqueRef();
        if (!"ADMIN".equals(role) && authBoutiqueId != null) {
            boutiqueRef = authBoutiqueId; // Override with auth boutique
            log.info("Forcing boutiqueRef={} for role={}", authBoutiqueId, role);
        }
        
        if (boutiqueRef == null || boutiqueRef.trim().isEmpty()) {
            throw new RuntimeException("boutiqueRef is required to create a customer");
        }
        
        Customer customer = Customer.builder()
            .customerRef("TMP-" + UUID.randomUUID().toString().substring(0, 32))
            .nom(request.nom())
            .prenom(request.prenom())
            .email(request.email())
            .telephone(request.telephone())
            .pieceIdentite(request.pieceIdentite())
            .adresse(request.adresse())
            .ville(request.ville())
            .codePostal(request.codePostal())
            .gouvernorat(request.gouvernorat())
            .pays(request.pays() != null ? request.pays() : "Tunisie")
            .type(Customer.ClientType.valueOf(request.type()))
            .status(Customer.ClientStatus.ACTIVE)
            .accountBalance(BigDecimal.ZERO)
            .creditLimit(request.creditLimit() != null ? request.creditLimit() : BigDecimal.valueOf(500))
            .hasSim(false)
            .boutiqueRef(boutiqueRef.trim())
            .build();
        
        customer = customerRepository.save(customer);
        customer.setCustomerRef(generateCustomerRef(customer.getId()));
        customer = customerRepository.save(customer);
        log.info("Created customer: {} (ref: {})", customer.getId(), customer.getCustomerRef());
        
        eventPublisher.publishCustomerCreated(customer);
        
        return toDto(customer);
    }

    private String generateCustomerRef(Long customerId) {
        return String.format("CLT-%d-%06d", Year.now().getValue(), customerId);
    }
    
    @Transactional
    public ClientDto updateCustomer(Long id, CreateClientRequest request) {
        Customer customer = customerRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Customer not found: " + id));
        
        customer.setNom(request.nom());
        customer.setPrenom(request.prenom());
        customer.setTelephone(request.telephone());
        customer.setAdresse(request.adresse());
        customer.setVille(request.ville());
        customer.setCodePostal(request.codePostal());
        customer.setGouvernorat(request.gouvernorat());
        if (request.pays() != null) {
            customer.setPays(request.pays());
        }
        
        customer = customerRepository.save(customer);
        log.info("Updated customer: {} (ref: {})", customer.getId(), customer.getCustomerRef());
        
        eventPublisher.publishCustomerUpdated(customer);
        
        return toDto(customer);
    }
    
    @Transactional
    public ClientDto updateCustomerByRef(String customerRef, CreateClientRequest request) {
        Customer customer = customerRepository.findByCustomerRef(customerRef)
            .orElseThrow(() -> new RuntimeException("Customer not found: " + customerRef));
        return updateCustomer(customer.getId(), request);
    }
    
    @Transactional
    public void suspendCustomer(Long id, String reason) {
        Customer customer = customerRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Customer not found: " + id));
        
        customer.setStatus(Customer.ClientStatus.SUSPENDED);
        customerRepository.save(customer);
        log.warn("Suspended customer: {} (ref: {}) - Reason: {}", 
            id, customer.getCustomerRef(), reason);
        
        eventPublisher.publishCustomerSuspended(customer);
    }
    
    @Transactional
    public void suspendCustomerByRef(String customerRef, String reason) {
        Customer customer = customerRepository.findByCustomerRef(customerRef)
            .orElseThrow(() -> new RuntimeException("Customer not found: " + customerRef));
        suspendCustomer(customer.getId(), reason);
    }
    
    @Transactional
    public ClientDto reactivateCustomer(Long id) {
        Customer customer = customerRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Customer not found: " + id));
        
        customer.setStatus(Customer.ClientStatus.ACTIVE);
        customer = customerRepository.save(customer);
        log.info("Reactivated customer: {} (ref: {})", id, customer.getCustomerRef());
        
        return toDto(customer);
    }
    
    @Transactional
    public ClientDto reactivateCustomerByRef(String customerRef) {
        Customer customer = customerRepository.findByCustomerRef(customerRef)
            .orElseThrow(() -> new RuntimeException("Customer not found: " + customerRef));
        return reactivateCustomer(customer.getId());
    }
    
    
    @Transactional(readOnly = true)
    public BigDecimal getBalance(String customerRef) {
        Customer customer = customerRepository.findByCustomerRef(customerRef)
            .orElseThrow(() -> new RuntimeException("Customer not found: " + customerRef));
        return customer.getAccountBalance();
    }
    
    @Transactional
    public ClientDto addCredit(String customerRef, BigDecimal amount, String description) {
        Customer customer = customerRepository.findByCustomerRef(customerRef)
            .orElseThrow(() -> new RuntimeException("Customer not found: " + customerRef));
        
        customer.addCredit(amount);
        customer = customerRepository.save(customer);
        log.info("Added credit to customer {}: {} ({})", customerRef, amount, description);
        
        return toDto(customer);
    }
    
    @Transactional
    public ClientDto deductCredit(String customerRef, BigDecimal amount, String description) {
        Customer customer = customerRepository.findByCustomerRef(customerRef)
            .orElseThrow(() -> new RuntimeException("Customer not found: " + customerRef));
        
        customer.deductCredit(amount);
        customer = customerRepository.save(customer);
        log.info("Deducted credit from customer {}: {} ({})", customerRef, amount, description);
        
        return toDto(customer);
    }
    
    @Transactional
    public ClientDto updateCreditLimit(String customerRef, BigDecimal newLimit) {
        Customer customer = customerRepository.findByCustomerRef(customerRef)
            .orElseThrow(() -> new RuntimeException("Customer not found: " + customerRef));
        
        customer.setCreditLimit(newLimit);
        customer = customerRepository.save(customer);
        log.info("Updated credit limit for customer {}: {}", customerRef, newLimit);
        
        return toDto(customer);
    }
    
    @Transactional(readOnly = true)
    public boolean canCharge(String customerRef, BigDecimal amount) {
        Customer customer = customerRepository.findByCustomerRef(customerRef)
            .orElseThrow(() -> new RuntimeException("Customer not found: " + customerRef));
        return customer.canCharge(amount);
    }
    
    
    private ClientDto toDto(Customer customer) {
        return new ClientDto(
            customer.getCustomerRef(),
            customer.getBoutiqueRef(),  // PUBLIC identifier only
            customer.getNom(),
            customer.getPrenom(),
            customer.getEmail(),
            customer.getTelephone(),
            customer.getPieceIdentite(),
            customer.getAdresse(),
            customer.getVille(),
            customer.getCodePostal(),
            customer.getGouvernorat(),
            customer.getPays(),
            customer.getType().name(),
            customer.getStatus().name(),
            customer.getAccountBalance(),
            customer.getCreditLimit(),
            customer.getHasSim(),
            customer.getCreatedAt()
        );
    }
}