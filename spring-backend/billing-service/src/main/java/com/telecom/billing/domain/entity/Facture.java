package com.telecom.billing.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "facture")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Facture {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "numero_facture", unique = true, nullable = false)
    private String numeroFacture;
    
    @Column(name = "client_id", nullable = false)
    private Long clientId;
    
    @Column(name = "abonnement_id")
    private Long abonnementId;
    
    @Column(name = "date_facture", nullable = false)
    private LocalDate dateFacture;
    
    @Column(name = "date_echeance", nullable = false)
    private LocalDate dateEcheance;
    
    @Column(name = "periode_debut")
    private LocalDate periodeDebut;
    
    @Column(name = "periode_fin")
    private LocalDate periodeFin;
    
    @Column(name = "montant_ht", precision = 15, scale = 2)
    private BigDecimal montantHT;
    
    @Column(name = "montant_tva", precision = 15, scale = 2)
    private BigDecimal montantTVA;
    
    @Column(name = "montant_ttc", precision = 15, scale = 2)
    private BigDecimal montantTTC;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FactureStatus statut;
    
    @OneToMany(mappedBy = "facture", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<InvoiceLine> lignes = new ArrayList<>();
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "paid_at")
    private LocalDateTime paidAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (statut == null) {
            statut = FactureStatus.DRAFT;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public void addLigne(InvoiceLine ligne) {
        lignes.add(ligne);
        ligne.setFacture(this);
    }
    
    public void removeLigne(InvoiceLine ligne) {
        lignes.remove(ligne);
        ligne.setFacture(null);
    }
    
    public void calculateTotals(BigDecimal taxRate) {
        this.montantHT = lignes.stream()
            .map(InvoiceLine::getMontant)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.montantTVA = BigDecimal.ZERO;
        this.montantTTC = this.montantHT;
    }
    
    public void finalize() {
        if (this.statut == FactureStatus.DRAFT) {
            this.statut = FactureStatus.PENDING;
        }
    }
    
    public void markAsSent() {
        if (this.statut == FactureStatus.PENDING) {
            this.statut = FactureStatus.SENT;
        }
    }
    
    public void markAsPaid() {
        this.statut = FactureStatus.PAID;
        this.paidAt = LocalDateTime.now();
    }
    
    public void markAsOverdue() {
        if (this.statut == FactureStatus.SENT && 
            LocalDate.now().isAfter(this.dateEcheance)) {
            this.statut = FactureStatus.OVERDUE;
        }
    }
    
    public void cancel() {
        this.statut = FactureStatus.CANCELLED;
    }
    
    public enum FactureStatus {
        DRAFT,
        PENDING,
        SENT,
        PAID,
        OVERDUE,
        CANCELLED
    }
}
