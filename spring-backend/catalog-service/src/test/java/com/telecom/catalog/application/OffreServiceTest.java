package com.telecom.catalog.application;

import com.telecom.catalog.domain.entity.Offre;
import com.telecom.catalog.domain.entity.Offre.OffreStatus;
import com.telecom.catalog.domain.entity.Offre.PaymentType;
import com.telecom.catalog.domain.entity.ServiceEntity;
import com.telecom.catalog.domain.entity.ServiceEntity.ServiceCategory;
import com.telecom.catalog.domain.repository.OffreRepository;
import com.telecom.catalog.domain.repository.ServiceRepository;
import com.telecom.catalog.web.dto.CreateOffreRequest;
import com.telecom.catalog.web.dto.OffreDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OffreServiceTest {

    @Mock
    private OffreRepository offreRepository;

    @Mock
    private ServiceRepository serviceRepository;

    @InjectMocks
    private OffreService offreService;

    private ServiceEntity serviceVoix;
    private Offre offreActive;
    private Offre offreInactive;
    private Offre offreExpiree;

    @BeforeEach
    void setUp() {
        serviceVoix = ServiceEntity.builder()
                .id(1L).code("VOICE").libelle("Appels voix")
                .unite("secondes").prixUnitaire(new BigDecimal("0.01"))
                .category(ServiceCategory.VOICE).active(true)
                .build();

        offreActive = Offre.builder()
                .id(1L).code("OFFRE-STD").libelle("Offre Standard")
                .description("Description standard")
                .prixMensuel(new BigDecimal("19.99"))
                .dateDebut(LocalDate.now().minusDays(10))
                .dateFin(LocalDate.now().plusDays(365))
                .status(OffreStatus.ACTIVE)
                .paymentType(PaymentType.POSTPAID)
                .services(Set.of(serviceVoix))
                .build();

        offreInactive = Offre.builder()
                .id(2L).code("OFFRE-OLD").libelle("Ancienne offre")
                .prixMensuel(new BigDecimal("9.99"))
                .status(OffreStatus.INACTIVE)
                .paymentType(PaymentType.POSTPAID)
                .services(Set.of())
                .build();

        offreExpiree = Offre.builder()
                .id(3L).code("OFFRE-EXP").libelle("Offre expirée")
                .prixMensuel(new BigDecimal("14.99"))
                .dateDebut(LocalDate.now().minusDays(365))
                .dateFin(LocalDate.now().minusDays(1))
                .status(OffreStatus.ACTIVE)
                .paymentType(PaymentType.POSTPAID)
                .services(Set.of())
                .build();
    }


    @Test
    void getAllOffres_retourneToutesLesOffres() {
        when(offreRepository.findAll()).thenReturn(List.of(offreActive, offreInactive));

        List<OffreDto> result = offreService.getAllOffres();

        assertEquals(2, result.size());
        verify(offreRepository).findAll();
    }
    @Test
    void getAllOffres_listeVide_retourneListeVide() {
        when(offreRepository.findAll()).thenReturn(List.of());

        List<OffreDto> result = offreService.getAllOffres();

        assertTrue(result.isEmpty());
    }


    @Test
    void getActiveOffres_retourneSeulementOffresActives() {
        when(offreRepository.findByStatus(OffreStatus.ACTIVE))
                .thenReturn(List.of(offreActive));

        List<OffreDto> result = offreService.getActiveOffres();

        assertEquals(1, result.size());
        assertEquals("OFFRE-STD", result.get(0).code());
        verify(offreRepository).findByStatus(OffreStatus.ACTIVE);
        verify(offreRepository, never()).findAll();
    }

    @Test
    void getActiveOffres_exclutOffresExpirees() {
        when(offreRepository.findByStatus(OffreStatus.ACTIVE))
                .thenReturn(List.of(offreActive, offreExpiree));

        List<OffreDto> result = offreService.getActiveOffres();

        assertEquals(1, result.size());
        assertEquals("OFFRE-STD", result.get(0).code());
    }

    @Test
    void getActiveOffres_aucuneOffre_retourneListeVide() {
        when(offreRepository.findByStatus(OffreStatus.ACTIVE)).thenReturn(List.of());

        List<OffreDto> result = offreService.getActiveOffres();

        assertTrue(result.isEmpty());
    }


    @Test
    void getOffreById_idExistant_retourneDto() {
        when(offreRepository.findById(1L)).thenReturn(Optional.of(offreActive));

        OffreDto result = offreService.getOffreById(1L);

        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals("OFFRE-STD", result.code());
        assertEquals(new BigDecimal("19.99"), result.prixMensuel());
    }

    @Test
    void getOffreById_idInexistant_leveRuntimeException() {
        when(offreRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> offreService.getOffreById(99L));

        assertTrue(ex.getMessage().contains("99"));
    }


    @Test
    void createOffre_avecServiceIdsValides_sauvegardeLOffre() {
        CreateOffreRequest request = new CreateOffreRequest(
                "OFFRE-NEW", "Nouvelle offre", "Description",
                new BigDecimal("29.99"),
                LocalDate.now(), LocalDate.now().plusDays(365),
                "POSTPAID", List.of(1L)
        );

        when(serviceRepository.findAllById(List.of(1L))).thenReturn(List.of(serviceVoix));
        when(offreRepository.save(any(Offre.class))).thenReturn(offreActive);

        OffreDto result = offreService.createOffre(request);

        assertNotNull(result);
        verify(serviceRepository).findAllById(List.of(1L));
        verify(offreRepository).save(any(Offre.class));
    }

    @Test
    void createOffre_sansServiceIds_creeOffreSansServices() {
        CreateOffreRequest request = new CreateOffreRequest(
                "OFFRE-SIMPLE", "Offre simple", null,
                new BigDecimal("9.99"),
                LocalDate.now(), LocalDate.now().plusDays(30),
                "PREPAID", null
        );

        when(offreRepository.save(any(Offre.class))).thenReturn(offreActive);

        offreService.createOffre(request);

        verify(serviceRepository, never()).findAllById(any());
        verify(offreRepository).save(any(Offre.class));
    }

    @Test
    void createOffre_paymentTypeNonSpecifie_utilisePOSTPAIDParDefaut() {
        CreateOffreRequest request = new CreateOffreRequest(
                "OFFRE-DEF", "Offre défaut", null,
                new BigDecimal("15.00"),
                LocalDate.now(), LocalDate.now().plusDays(30),
                null, null
        );

        when(offreRepository.save(any(Offre.class))).thenReturn(offreActive);

        OffreDto result = offreService.createOffre(request);

        assertNotNull(result);
        verify(offreRepository).save(any(Offre.class));
    }


    @Test
    void updateOffre_idExistant_metAJourSeulementLesChampsFournis() {
        CreateOffreRequest request = new CreateOffreRequest(
                null, "Libelle mis à jour", null,
                null, null, null, null, null
        );

        when(offreRepository.findById(1L)).thenReturn(Optional.of(offreActive));
        when(offreRepository.save(any(Offre.class))).thenReturn(offreActive);

        offreService.updateOffre(1L, request);

        verify(offreRepository).findById(1L);
        verify(offreRepository).save(offreActive);
        verify(serviceRepository, never()).findAllById(any());
        
    }

    @Test
    void updateOffre_avecNouveauxServiceIds_metAJourLesServices() {
        CreateOffreRequest request = new CreateOffreRequest(
                null, null, null,
                null, null, null, null, List.of(1L)
        );

        when(offreRepository.findById(1L)).thenReturn(Optional.of(offreActive));
        when(serviceRepository.findAllById(List.of(1L))).thenReturn(List.of(serviceVoix));
        when(offreRepository.save(any(Offre.class))).thenReturn(offreActive);

        offreService.updateOffre(1L, request);

        verify(serviceRepository).findAllById(List.of(1L));
        verify(offreRepository).save(offreActive);
    }

    @Test
    void updateOffre_idInexistant_leveException() {
        CreateOffreRequest request = new CreateOffreRequest(
                null, "X", null, null, null, null, null, null
        );

        when(offreRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> offreService.updateOffre(99L, request));

        verify(offreRepository, never()).save(any());
    }


    @Test
    void deleteOffre_idExistant_supprimeLOffre() {
        when(offreRepository.findById(1L)).thenReturn(Optional.of(offreActive));

        offreService.deleteOffre(1L);
        
        verify(offreRepository).save(offreActive);
    }

    @Test
    void deleteOffre_idInexistant_leveException() {
        when(offreRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> offreService.deleteOffre(99L));

        verify(offreRepository, never()).delete(any());
    }
}
