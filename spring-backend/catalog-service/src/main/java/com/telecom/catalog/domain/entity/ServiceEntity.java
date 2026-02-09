package com.telecom.catalog.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "service")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String code;
    
    @Column(nullable = false)
    private String libelle;
    
    @Column(nullable = false)
    private String unite;
    
    @Column(name = "prix_unitaire", nullable = false)
    private BigDecimal prixUnitaire;
    
    @Enumerated(EnumType.STRING)
    private ServiceCategory category;
    
    private boolean active;
    
    @PrePersist
    protected void onCreate() {
        active = true;
    }
    
    public BigDecimal calculateCharge(BigDecimal quantity) {
        return prixUnitaire.multiply(quantity);
    }
    
    public enum ServiceCategory {
        VOICE, DATA, SMS, ROAMING, VALUE_ADDED
    }
}
