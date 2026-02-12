package com.telecom.usage.infrastructure.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;
import java.util.List;

@FeignClient(name = "catalog-service", url = "${services.catalog-url}")
public interface CatalogClient {

    @GetMapping("/offres/{id}")
    OffreDto getOffreById(@PathVariable("id") Long id);

    record OffreDto(
            Long id, String code, String libelle, String description,
            BigDecimal prixMensuel, String dateDebut, String dateFin,
            String status, String paymentType, List<Long> serviceIds
    ) {}
}
