package com.telecom.boutique.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Tracks SIM card inventory per boutique.
 */
@Entity
@Table(name = "stock_sim", indexes = {
    @Index(name = "idx_stock_boutique", columnList = "boutique_id"),
    @Index(name = "idx_stock_iccid", columnList = "iccid", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockSim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String iccid;

    @Column(length = 20)
    private String imsi;

    @Column(length = 20)
    private String msisdn;

    @Enumerated(EnumType.STRING)
    @Column(name = "sim_type", nullable = false, columnDefinition = "VARCHAR(50)")
    private SimType simType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(50)")
    private SimStatus status;

    @Column(name = "boutique_id", nullable = false)
    private Long boutiqueId;

    @Column(name = "assigned_to_client_id")
    private Long assignedToClientId;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) status = SimStatus.AVAILABLE;
    }

    public enum SimType {
        STANDARD, ESIM
    }

    public enum SimStatus {
        AVAILABLE, ASSIGNED, ACTIVATED, SUSPENDED, DEACTIVATED, DAMAGED, LOST
    }
}
