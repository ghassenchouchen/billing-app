package com.telecom.customer.application;

import com.telecom.customer.domain.entity.Client;
import com.telecom.customer.domain.repository.ClientRepository;
import com.telecom.customer.infrastructure.kafka.CustomerEventPublisher;
import com.telecom.customer.web.dto.ClientDto;
import com.telecom.customer.web.dto.CreateClientRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerService {
    
    private final ClientRepository clientRepository;
    private final CustomerEventPublisher eventPublisher;
    
    @Transactional(readOnly = true)
    public List<ClientDto> getAllCustomers() {
        return clientRepository.findAll().stream()
            .map(this::toDto)
            .toList();
    }
    
    @Transactional(readOnly = true)
    public ClientDto getCustomerById(Long id) {
        return clientRepository.findById(id)
            .map(this::toDto)
            .orElseThrow(() -> new RuntimeException("Customer not found: " + id));
    }
    
    @Transactional(readOnly = true)
    public ClientDto getCustomerByEmail(String email) {
        return clientRepository.findByEmail(email)
            .map(this::toDto)
            .orElseThrow(() -> new RuntimeException("Customer not found: " + email));
    }
    
    @Transactional
    public ClientDto createCustomer(CreateClientRequest request) {
        if (clientRepository.existsByEmail(request.email())) {
            throw new RuntimeException("Email already exists: " + request.email());
        }
        
        Client client = Client.builder()
            .nom(request.nom())
            .prenom(request.prenom())
            .email(request.email())
            .telephone(request.telephone())
            .adresse(request.adresse())
            .type(Client.ClientType.valueOf(request.type()))
            .status(Client.ClientStatus.ACTIVE)
            .build();
        
        client = clientRepository.save(client);
        log.info("Created customer: {}", client.getId());
        
        // Publish event
        eventPublisher.publishCustomerCreated(client);
        
        return toDto(client);
    }
    
    @Transactional
    public ClientDto updateCustomer(Long id, CreateClientRequest request) {
        Client client = clientRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Customer not found: " + id));
        
        client.setNom(request.nom());
        client.setPrenom(request.prenom());
        client.setTelephone(request.telephone());
        client.setAdresse(request.adresse());
        
        client = clientRepository.save(client);
        log.info("Updated customer: {}", client.getId());
        
        // Publish event
        eventPublisher.publishCustomerUpdated(client);
        
        return toDto(client);
    }
    
    @Transactional
    public void suspendCustomer(Long id) {
        Client client = clientRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Customer not found: " + id));
        
        client.setStatus(Client.ClientStatus.SUSPENDED);
        clientRepository.save(client);
        log.info("Suspended customer: {}", id);
        
        eventPublisher.publishCustomerSuspended(client);
    }
    
    private ClientDto toDto(Client client) {
        return new ClientDto(
            client.getId(),
            client.getNom(),
            client.getPrenom(),
            client.getEmail(),
            client.getTelephone(),
            client.getAdresse(),
            client.getType().name(),
            client.getStatus().name(),
            client.getCreatedAt()
        );
    }
}
