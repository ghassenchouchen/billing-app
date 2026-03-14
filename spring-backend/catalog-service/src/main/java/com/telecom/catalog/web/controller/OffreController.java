package com.telecom.catalog.web.controller;

import com.telecom.catalog.application.OffreService;
import com.telecom.catalog.web.dto.OffreDto;
import com.telecom.catalog.web.dto.CreateOffreRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/offres")
@RequiredArgsConstructor
public class OffreController {
    
    private final OffreService offreService;
    
    @GetMapping
    public ResponseEntity<List<OffreDto>> getAllOffres() {
        return ResponseEntity.ok(offreService.getAllOffres());
    }
    
    @GetMapping("/active")
    public ResponseEntity<List<OffreDto>> getActiveOffres() {
        return ResponseEntity.ok(offreService.getActiveOffres());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<OffreDto> getOffreById(@PathVariable Long id) {
        return ResponseEntity.ok(offreService.getOffreById(id));
    }
    
    @PostMapping
    public ResponseEntity<OffreDto> createOffre(@Valid @RequestBody CreateOffreRequest request) {
        return ResponseEntity.ok(offreService.createOffre(request));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<OffreDto> updateOffre(@PathVariable Long id, @Valid @RequestBody CreateOffreRequest request) {
        return ResponseEntity.ok(offreService.updateOffre(id, request));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOffre(@PathVariable Long id) {
        offreService.deleteOffre(id);
        return ResponseEntity.noContent().build();
    }
}
