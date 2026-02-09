package com.telecom.catalog.application;

import com.telecom.catalog.domain.entity.ServiceEntity;
import com.telecom.catalog.domain.repository.ServiceRepository;
import com.telecom.catalog.web.dto.ServiceDto;
import com.telecom.catalog.web.dto.CreateServiceRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CatalogService {
    
    private final ServiceRepository serviceRepository;
    
    @Transactional(readOnly = true)
    public List<ServiceDto> getAllServices() {
        return serviceRepository.findAll().stream()
            .map(this::toDto)
            .toList();
    }
    
    @Transactional(readOnly = true)
    public List<ServiceDto> getActiveServices() {
        return serviceRepository.findByActiveTrue().stream()
            .map(this::toDto)
            .toList();
    }
    
    @Transactional(readOnly = true)
    public ServiceDto getServiceById(Long id) {
        return serviceRepository.findById(id)
            .map(this::toDto)
            .orElseThrow(() -> new RuntimeException("Service not found: " + id));
    }
    
    @Transactional(readOnly = true)
    public ServiceDto getServiceByCode(String code) {
        return serviceRepository.findByCode(code)
            .map(this::toDto)
            .orElseThrow(() -> new RuntimeException("Service not found: " + code));
    }
    
    @Transactional
    public ServiceDto createService(CreateServiceRequest request) {
        ServiceEntity service = ServiceEntity.builder()
            .code(request.code())
            .libelle(request.libelle())
            .unite(request.unite())
            .prixUnitaire(request.prixUnitaire())
            .category(ServiceEntity.ServiceCategory.valueOf(request.category()))
            .active(true)
            .build();
        
        service = serviceRepository.save(service);
        log.info("Created service: {}", service.getId());
        
        return toDto(service);
    }
    
    @Transactional
    public ServiceDto updateService(Long id, CreateServiceRequest request) {
        ServiceEntity service = serviceRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Service not found: " + id));
        
        service.setLibelle(request.libelle());
        service.setUnite(request.unite());
        service.setPrixUnitaire(request.prixUnitaire());
        service.setCategory(ServiceEntity.ServiceCategory.valueOf(request.category()));
        
        service = serviceRepository.save(service);
        log.info("Updated service: {}", service.getId());
        
        return toDto(service);
    }
    
    @Transactional
    public void deleteService(Long id) {
        ServiceEntity service = serviceRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Service not found: " + id));
        serviceRepository.delete(service);
        log.info("Deleted service: {}", id);
    }
    
    private ServiceDto toDto(ServiceEntity service) {
        return new ServiceDto(
            service.getId(),
            service.getCode(),
            service.getLibelle(),
            service.getUnite(),
            service.getPrixUnitaire(),
            service.getCategory() != null ? service.getCategory().name() : null,
            service.isActive()
        );
    }
}
