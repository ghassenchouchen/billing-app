package com.telecom.boutique.application;

import com.telecom.boutique.domain.entity.Boutique;
import com.telecom.boutique.domain.exception.BoutiqueNotFoundException;
import com.telecom.boutique.domain.exception.DuplicateResourceException;
import com.telecom.boutique.domain.repository.BoutiqueRepository;
import com.telecom.boutique.web.dto.BoutiqueDto;
import com.telecom.boutique.web.dto.CreateBoutiqueRequest;
import com.telecom.boutique.web.dto.UpdateBoutiqueRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Manages boutique lifecycle: create, read, update, deactivate.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BoutiqueService {

    private final BoutiqueRepository boutiqueRepository;

    @Transactional(readOnly = true)
    public List<BoutiqueDto> getAllBoutiques() {
        return boutiqueRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public BoutiqueDto getBoutiqueById(Long id) {
        return boutiqueRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new BoutiqueNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public BoutiqueDto getBoutiqueByCode(String code) {
        return boutiqueRepository.findByCode(code)
                .map(this::toDto)
                .orElseThrow(() -> new BoutiqueNotFoundException(code));
    }

    @Transactional
    public BoutiqueDto createBoutique(CreateBoutiqueRequest req) {
        if (boutiqueRepository.existsByCode(req.code())) {
            throw new DuplicateResourceException("Boutique code already exists: " + req.code());
        }
        Boutique b = Boutique.builder()
                .code(req.code())
                .nom(req.nom())
                .adresse(req.adresse())
                .ville(req.ville())
                .codePostal(req.codePostal())
                .telephone(req.telephone())
                .email(req.email())
                .responsableId(req.responsableId())
                .status(Boutique.BoutiqueStatus.ACTIVE)
                .build();
        b = boutiqueRepository.save(b);
        log.info("Created boutique: {} ({})", b.getId(), b.getCode());
        return toDto(b);
    }

    @Transactional
    public BoutiqueDto updateBoutique(Long id, UpdateBoutiqueRequest req) {
        Boutique b = boutiqueRepository.findById(id)
                .orElseThrow(() -> new BoutiqueNotFoundException(id));

        if (req.nom() != null) b.setNom(req.nom());
        if (req.adresse() != null) b.setAdresse(req.adresse());
        if (req.ville() != null) b.setVille(req.ville());
        if (req.codePostal() != null) b.setCodePostal(req.codePostal());
        if (req.telephone() != null) b.setTelephone(req.telephone());
        if (req.email() != null) b.setEmail(req.email());
        if (req.responsableId() != null) b.setResponsableId(req.responsableId());
        if (req.status() != null) {
            b.setStatus(Boutique.BoutiqueStatus.valueOf(req.status()));
        }

        b = boutiqueRepository.save(b);
        log.info("Updated boutique: {} ({})", b.getId(), b.getCode());
        return toDto(b);
    }

    @Transactional
    public void deactivateBoutique(Long id) {
        Boutique b = boutiqueRepository.findById(id)
                .orElseThrow(() -> new BoutiqueNotFoundException(id));
        b.setStatus(Boutique.BoutiqueStatus.INACTIVE);
        boutiqueRepository.save(b);
        log.info("Deactivated boutique: {} ({})", b.getId(), b.getCode());
    }

    private BoutiqueDto toDto(Boutique b) {
        return new BoutiqueDto(b.getId(), b.getCode(), b.getNom(), b.getAdresse(), b.getVille(),
                b.getCodePostal(), b.getTelephone(), b.getEmail(), b.getResponsableId(),
                b.getStatus().name(), b.getCreatedAt());
    }
}
