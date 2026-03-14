package com.telecom.subscription.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "abonnement")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Abonnement {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "client_id")
    private Long clientId;

    @Column(name = "client_ref", length = 64)
    private String clientRef;
    
    @Column(name = "offre_id", nullable = false)
    private Long offreId;
    
    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;
    
    @Column(name = "date_fin")
    private LocalDate dateFin;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AbonnementStatus status;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "billing_frequency", nullable = false)
    @Builder.Default
    private BillingFrequency billingFrequency = BillingFrequency.MONTHLY;
    
    @Column(name = "last_billing_date")
    private LocalDate lastBillingDate;
    
    @Column(name = "next_billing_date")
    private LocalDate nextBillingDate;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = AbonnementStatus.ACTIVE;
        }
        if (billingFrequency == null) {
            billingFrequency = BillingFrequency.MONTHLY;
        }
        if (nextBillingDate == null && dateDebut != null) {
            nextBillingDate = billingFrequency.getNextBillingDate(dateDebut);
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public boolean isActive() {
        return status == AbonnementStatus.ACTIVE && !isExpired();
    }
    
    public boolean isExpired() {
        return dateFin != null && LocalDate.now().isAfter(dateFin);
    }
    
    public void activate() {
        if (status == AbonnementStatus.PENDING || status == AbonnementStatus.SUSPENDED) {
            this.status = AbonnementStatus.ACTIVE;
        }
    }
    
    public void suspend() {
        if (status == AbonnementStatus.ACTIVE) {
            this.status = AbonnementStatus.SUSPENDED;
        }
    }
    
    public void terminate() {
        this.status = AbonnementStatus.TERMINATED;
        this.dateFin = LocalDate.now();
    }
    
    /**
     * Update billing dates after invoice generation
     */
    public void updateBillingDates(LocalDate billingDate) {
        this.lastBillingDate = billingDate;
        this.nextBillingDate = billingFrequency.getNextBillingDate(billingDate);
    }
    
    /**
     * Check if subscription is due for billing
     */
    public boolean isDueForBilling() {
        if (status != AbonnementStatus.ACTIVE) {
            return false;
        }
        LocalDate now = LocalDate.now();
        return nextBillingDate == null || !now.isBefore(nextBillingDate);
    }
    
    public enum AbonnementStatus {
        PENDING, ACTIVE, SUSPENDED, TERMINATED
    }
}
