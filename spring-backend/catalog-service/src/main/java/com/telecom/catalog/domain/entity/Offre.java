package com.telecom.catalog.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "offre")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Offre {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String code;
    
    @Column(nullable = false)
    private String libelle;
    
    private String description;
    
    @Column(name = "prix_mensuel", nullable = false)
    private BigDecimal prixMensuel;
    
    @Column(name = "date_debut")
    private LocalDate dateDebut;
    
    @Column(name = "date_fin")
    private LocalDate dateFin;
    
    @Enumerated(EnumType.STRING)
    private OffreStatus status;
    
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "offre_service",
        joinColumns = @JoinColumn(name = "offre_id"),
        inverseJoinColumns = @JoinColumn(name = "service_id")
    )
    @Builder.Default
    private Set<ServiceEntity> services = new HashSet<>();
    
    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = OffreStatus.ACTIVE;
        }
    }
    
    public boolean isValid() {
        LocalDate now = LocalDate.now();
        return status == OffreStatus.ACTIVE &&
               (dateDebut == null || !now.isBefore(dateDebut)) &&
               (dateFin == null || !now.isAfter(dateFin));
    }
    
    public enum OffreStatus {
        ACTIVE, INACTIVE, DISCONTINUED
    }
}
