package com.telecom.billing.infrastructure.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "customer-service", url = "${services.customer-url}")
public interface CustomerClient {

    @GetMapping("/customers/{id}")
    ClientDto getCustomerById(@PathVariable("id") Long id);

    record ClientDto(
        String customerRef,
        String boutiqueRef,
        String nom,
        String prenom,
        String email,
        String telephone,
        String pieceIdentite,
        String adresse,
        String ville,
        String codePostal,
        String gouvernorat,
        String pays,
        String type,
        String status
    ) {
        public String fullName() {
            if ("BUSINESS".equals(type)) return nom;
            return (prenom != null ? prenom + " " : "") + (nom != null ? nom : "");
        }

        public String fullAddress() {
            StringBuilder sb = new StringBuilder();
            if (adresse != null) sb.append(adresse);
            if (ville != null) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(ville);
            }
            if (codePostal != null) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(codePostal);
            }
            return sb.toString();
        }
    }
}
