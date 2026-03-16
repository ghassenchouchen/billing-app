package com.telecom.billing.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "invoice_line")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceLine {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facture_id", nullable = false)
    private Facture facture;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LineType type;
    
    @Column(nullable = false)
    private String description;
    
    @Column(name = "service_id")
    private Long serviceId;
    
    @Column(name = "usage_id")
    private Long usageId;
    
    private Integer quantite;
    
    @Column(name = "prix_unitaire", precision = 15, scale = 4)
    private BigDecimal prixUnitaire;
    
    @Column(precision = 15, scale = 2)
    private BigDecimal montant;
    
    public enum LineType {
        SUBSCRIPTION,   // Abonnement mensuel
        USAGE_VOICE,    // Consommation voix
        USAGE_SMS,      // Consommation SMS
        USAGE_DATA,     // Consommation données
        SETUP_FEE,      // Frais d'installation
        PENALTY,        // Pénalité
        DISCOUNT,       // Remise
        ADJUSTMENT      // Ajustement
    }
    
    public void calculateMontant() {
        if (quantite != null && prixUnitaire != null) {
            this.montant = prixUnitaire.multiply(BigDecimal.valueOf(quantite));
        }
    }
}
