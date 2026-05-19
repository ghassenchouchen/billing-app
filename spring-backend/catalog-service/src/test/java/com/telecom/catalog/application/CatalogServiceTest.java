package com.telecom.catalog.application;

import com.telecom.catalog.domain.entity.ServiceEntity;
import com.telecom.catalog.domain.entity.ServiceEntity.ServiceCategory;
import com.telecom.catalog.domain.repository.ServiceRepository;
import com.telecom.catalog.web.dto.CreateServiceRequest;
import com.telecom.catalog.web.dto.ServiceDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CatalogServiceTest {

    @Mock
    private ServiceRepository serviceRepository;

    @InjectMocks
    private CatalogService catalogService;

    private ServiceEntity serviceVoix;
    private ServiceEntity serviceData;

    @BeforeEach
    void setUp() {
        serviceVoix = ServiceEntity.builder()
                .id(1L)
                .code("VOICE")
                .libelle("Appels voix")
                .unite("secondes")
                .prixUnitaire(new BigDecimal("0.01"))
                .category(ServiceCategory.VOICE)
                .active(true)
                .build();

        serviceData = ServiceEntity.builder()
                .id(2L)
                .code("DATA")
                .libelle("Data mobile")
                .unite("MB")
                .prixUnitaire(new BigDecimal("0.02"))
                .category(ServiceCategory.DATA)
                .active(true)
                .build();
    }


    @Test
    void getAllServices_retourneTousLesServices() {
        when(serviceRepository.findAll()).thenReturn(List.of(serviceVoix, serviceData));

        List<ServiceDto> result = catalogService.getAllServices();

        assertEquals(2, result.size());
        assertEquals("VOICE", result.get(0).code());
        assertEquals("DATA", result.get(1).code());
        verify(serviceRepository).findAll();
    }

    @Test
    void getAllServices_listeVide_retourneListeVide() {
        when(serviceRepository.findAll()).thenReturn(List.of());

        List<ServiceDto> result = catalogService.getAllServices();

        assertTrue(result.isEmpty());
    }


    @Test
    void getActiveServices_retourneSeulementLesActifs() {
        when(serviceRepository.findByActiveTrue()).thenReturn(List.of(serviceVoix));

        List<ServiceDto> result = catalogService.getActiveServices();

        assertEquals(1, result.size());
        assertEquals("VOICE", result.get(0).code());
        verify(serviceRepository).findByActiveTrue();
        verify(serviceRepository, never()).findAll();
    }


    @Test
    void getServiceById_idExistant_retourneDto() {
        when(serviceRepository.findById(1L)).thenReturn(Optional.of(serviceVoix));

        ServiceDto result = catalogService.getServiceById(1L);

        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals("VOICE", result.code());
        assertEquals("Appels voix", result.libelle());
    }

    @Test
    void getServiceById_idInexistant_leveRuntimeException() {
        when(serviceRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> catalogService.getServiceById(99L));

        assertTrue(ex.getMessage().contains("99"));
    }


    @Test
    void getServiceByCode_codeExistant_retourneDto() {
        when(serviceRepository.findByCode("VOICE")).thenReturn(Optional.of(serviceVoix));

        ServiceDto result = catalogService.getServiceByCode("VOICE");

        assertNotNull(result);
        assertEquals("VOICE", result.code());
    }

    @Test
    void getServiceByCode_codeInexistant_leveRuntimeException() {
        when(serviceRepository.findByCode("INCONNU")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> catalogService.getServiceByCode("INCONNU"));
    }


    @Test
    void createService_requeteValide_sauvegardeetRetourneDto() {
        CreateServiceRequest request = new CreateServiceRequest(
                "SMS", "Messages SMS", "unité",
                new BigDecimal("0.05"), "SMS"
        );

        ServiceEntity saved = ServiceEntity.builder()
                .id(10L).code("SMS").libelle("Messages SMS")
                .unite("unité").prixUnitaire(new BigDecimal("0.05"))
                .category(ServiceCategory.SMS).active(true)
                .build();

        when(serviceRepository.save(any(ServiceEntity.class))).thenReturn(saved);

        ServiceDto result = catalogService.createService(request);

        assertNotNull(result);
        assertEquals(10L, result.id());
        assertEquals("SMS", result.code());
        verify(serviceRepository).save(any(ServiceEntity.class));
    }


    @Test
    void updateService_idExistant_metAJourLesChamps() {
        CreateServiceRequest request = new CreateServiceRequest(
                "VOICE", "Appels voix mis à jour", "minutes",
                new BigDecimal("0.015"), "VOICE"
        );

        when(serviceRepository.findById(1L)).thenReturn(Optional.of(serviceVoix));
        when(serviceRepository.save(any(ServiceEntity.class))).thenReturn(serviceVoix);

        ServiceDto result = catalogService.updateService(1L, request);

        assertNotNull(result);
        verify(serviceRepository).findById(1L);
        verify(serviceRepository).save(serviceVoix);
    }

    @Test
    void updateService_idInexistant_leveException() {
        CreateServiceRequest request = new CreateServiceRequest(
                "X", "X", "X", BigDecimal.ONE, "VOICE"
        );

        when(serviceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> catalogService.updateService(99L, request));

        verify(serviceRepository, never()).save(any());
    }


    @Test
    void deleteService_idExistant_supprimeLEntite() {
        when(serviceRepository.findById(1L)).thenReturn(Optional.of(serviceVoix));

        catalogService.deleteService(1L);

        verify(serviceRepository).save(serviceVoix);
    }

    @Test
    void deleteService_idInexistant_leveException() {
        when(serviceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> catalogService.deleteService(99L));

        verify(serviceRepository, never()).delete(any());
    }
}
