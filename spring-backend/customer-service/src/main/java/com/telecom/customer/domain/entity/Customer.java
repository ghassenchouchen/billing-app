package com.telecom.customer.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
/* Customers can be individuals or businesses */
 

@Entity
@Table(name = "client", indexes = {
        @Index(name = "idx_customer_ref", columnList = "customer_ref"),
        @Index(name = "idx_client_boutique_ref", columnList = "boutique_ref")
})
@Data   
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "customer_ref", nullable = false, unique=true, length = 36)
    private String customerRef;
    @Column(name= "boutique_ref", nullable = false)
    private String boutiqueRef;
    @Column(nullable = false)
    private String nom;
    
    @Column(nullable = false)
    private String prenom;
    
    @Column(nullable = false, unique = true)
    private String email;
    // contact phone
    private String telephone;

    @Column(name = "piece_identite", length = 20)
    private String pieceIdentite;   // CIN / Passport number
    
    private String adresse;
    private String ville;
    private String codePostal;
    private String gouvernorat;
    private String pays;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50, columnDefinition = "VARCHAR(50)")
    // can be individual or business
    private ClientType type;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50, columnDefinition = "VARCHAR(50)")
    private ClientStatus status;
    // account balance ( can be negative= need to pay money or positive = credit)
    @Column(precision = 10, scale =2 , nullable = false)
    private BigDecimal  accountBalance;
    // maximum credit allowed for postpaid customers
    @Column (precision = 10, scale =2, nullable =   false)
    private BigDecimal creditLimit;

    @Column(name = "has_sim", nullable = false)
    private Boolean hasSim;

    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = ClientStatus.ACTIVE;
        }
        if (accountBalance == null) {
            accountBalance = BigDecimal.ZERO;
        }
        if (creditLimit == null) {
            creditLimit = BigDecimal.ZERO;
        }
        if (hasSim == null) {
            hasSim = false;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
       
        updatedAt = LocalDateTime.now();
    }
    public boolean canCharge(BigDecimal amount)
    {
        // Check if account balance allows the charge
        // Positive balance = prepaid credit available
        // Negative balance = postpaid debt — compare against credit limit
        if (accountBalance.compareTo(amount) >= 0) {
            return true;
        }
        // Postpaid: check credit limit
        BigDecimal newBalance = accountBalance.subtract(amount);
        return newBalance.negate().compareTo(creditLimit) <= 0;
    }
        public void addCredit(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        this.accountBalance = this.accountBalance.add(amount);
    }
    public void deductCredit(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (!canCharge(amount)) {
            throw new IllegalStateException("Insufficient balance/credit");
        }
        this.accountBalance = this.accountBalance.subtract(amount);
    }


    
    public enum ClientType {
        INDIVIDUAL, BUSINESS
    }
    
    public enum ClientStatus {
        ACTIVE, SUSPENDED, CLOSED
    }
}
