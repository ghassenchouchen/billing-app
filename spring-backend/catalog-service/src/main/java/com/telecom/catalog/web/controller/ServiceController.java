package com.telecom.catalog.web.controller;

import com.telecom.catalog.application.CatalogService;
import com.telecom.catalog.web.dto.ServiceDto;
import com.telecom.catalog.web.dto.CreateServiceRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/services")
@RequiredArgsConstructor
public class ServiceController {
    
    private final CatalogService catalogService;
    
    @GetMapping
    public ResponseEntity<List<ServiceDto>> getAllServices() {
        return ResponseEntity.ok(catalogService.getAllServices());
    }
    
    @GetMapping("/active")
    public ResponseEntity<List<ServiceDto>> getActiveServices() {
        return ResponseEntity.ok(catalogService.getActiveServices());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ServiceDto> getServiceById(@PathVariable Long id) {
        return ResponseEntity.ok(catalogService.getServiceById(id));
    }
    
    @GetMapping("/code/{code}")
    public ResponseEntity<ServiceDto> getServiceByCode(@PathVariable String code) {
        return ResponseEntity.ok(catalogService.getServiceByCode(code));
    }
    
    @PostMapping
    public ResponseEntity<ServiceDto> createService(@RequestBody CreateServiceRequest request) {
        return ResponseEntity.ok(catalogService.createService(request));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ServiceDto> updateService(
            @PathVariable Long id,
            @RequestBody CreateServiceRequest request) {
        return ResponseEntity.ok(catalogService.updateService(id, request));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteService(@PathVariable Long id) {
        catalogService.deleteService(id);
        return ResponseEntity.noContent().build();
    }
}
