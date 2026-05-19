package com.telecom.catalog.application;

import com.telecom.catalog.domain.entity.Offre;
import com.telecom.catalog.domain.entity.Offre.PaymentType;
import com.telecom.catalog.domain.entity.ServiceEntity;
import com.telecom.catalog.domain.repository.OffreRepository;
import com.telecom.catalog.domain.repository.ServiceRepository;
import com.telecom.catalog.web.dto.OffreDto;
import com.telecom.catalog.web.dto.CreateOffreRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class OffreService {
    
    private final OffreRepository offreRepository;
    private final ServiceRepository serviceRepository;
    
    @Transactional(readOnly = true)
    public List<OffreDto> getAllOffres() {
        return offreRepository.findAll().stream()
            .map(this::toDto)
            .toList();
    }
    
    @Transactional(readOnly = true)
    public List<OffreDto> getActiveOffres() {
        return offreRepository.findByStatus(Offre.OffreStatus.ACTIVE).stream()
            .filter(Offre::isValid)
            .map(this::toDto)
            .toList();
    }
    
    @Transactional(readOnly = true)
    public OffreDto getOffreById(Long id) {
        return offreRepository.findById(id)
            .map(this::toDto)
            .orElseThrow(() -> new RuntimeException("Offre not found: " + id));
    }
    
    @Transactional
    public OffreDto createOffre(CreateOffreRequest request) {
        Set<ServiceEntity> services = new HashSet<>();
        if (request.serviceIds() != null) {
            services.addAll(serviceRepository.findAllById(request.serviceIds()));
        }
        
        Offre offre = Offre.builder()
            .code(request.code())
            .libelle(request.libelle())
            .description(request.description())
            .prixMensuel(request.prixMensuel())
            .dateDebut(request.dateDebut())
            .dateFin(request.dateFin())
            .status(Offre.OffreStatus.ACTIVE)
            .paymentType(request.paymentType() != null ? PaymentType.valueOf(request.paymentType()) : PaymentType.POSTPAID)
            .services(services)
            .build();
        
        offre = offreRepository.save(offre);
        log.info("Created offre: {}", offre.getId());
        
        return toDto(offre);
    }
    
    @Transactional
    public OffreDto updateOffre(Long id, CreateOffreRequest request) {
        Offre offre = offreRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Offre not found: " + id));
        
        if (request.libelle() != null) offre.setLibelle(request.libelle());
        if (request.description() != null) offre.setDescription(request.description());
        if (request.prixMensuel() != null) offre.setPrixMensuel(request.prixMensuel());
        if (request.dateDebut() != null) offre.setDateDebut(request.dateDebut());
        if (request.dateFin() != null) offre.setDateFin(request.dateFin());
        if (request.paymentType() != null) offre.setPaymentType(PaymentType.valueOf(request.paymentType()));
        
        if (request.serviceIds() != null) {
            Set<ServiceEntity> services = new HashSet<>(serviceRepository.findAllById(request.serviceIds()));
            offre.setServices(services);
        }
        
        offre = offreRepository.save(offre);
        log.info("Updated offre: {}", offre.getId());
        
        return toDto(offre);
    }
    
    @Transactional
    public void deleteOffre(Long id) {
        Offre offre = offreRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Offre not found: " + id));
        // SOFT DELETE: Preserve data integrity for existing subscriptions and billing history
        offre.setStatus(Offre.OffreStatus.DISCONTINUED);
        offreRepository.save(offre);
        log.info("Soft deleted offre (marked NO longer available): {}", id);
    }
    
    private OffreDto toDto(Offre offre) {
        return new OffreDto(
            offre.getId(),
            offre.getCode(),
            offre.getLibelle(),
            offre.getDescription(),
            offre.getPrixMensuel(),
            offre.getDateDebut(),
            offre.getDateFin(),
            offre.getStatus().name(),
            offre.getPaymentType() != null ? offre.getPaymentType().name() : "POSTPAID",
            offre.getServices().stream().map(ServiceEntity::getId).toList()
        );
    }
}
