package com.telecom.boutique.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Records every sale / subscription made in a boutique.
 */
@Entity
@Table(name = "transaction_boutique", indexes = {
    @Index(name = "idx_txn_boutique", columnList = "boutique_id"),
    @Index(name = "idx_txn_agent", columnList = "agent_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionBoutique {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reference", nullable = false, unique = true, length = 30)
    private String reference;

    @Column(name = "boutique_id", nullable = false)
    private Long boutiqueId;

    @Column(name = "agent_id")
    private Long agentId;

    @Column(name = "client_id")
    private Long clientId;

    @Column(name = "client_nom")
    private String clientNom;

    @Column(name = "offre_libelle")
    private String offreLibelle;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_transaction", nullable = false, columnDefinition = "VARCHAR(50)")
    private TransactionType typeTransaction;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal montant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(50)")
    private TransactionStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) status = TransactionStatus.PENDING;
    }

    public enum TransactionType {
        NEW_SUBSCRIPTION, RENEWAL, SIM_SWAP, ACCESSORY_SALE, TOP_UP, CANCELLATION, INVOICE_PAYMENT
    }

    public enum TransactionStatus {
        PENDING, COMPLETED, CANCELLED
    }
}
