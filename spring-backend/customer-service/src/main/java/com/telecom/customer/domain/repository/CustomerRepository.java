package com.telecom.customer.domain.repository;

import com.telecom.customer.domain.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Client, Long> {
    
    Optional<Client> findByEmail(String email);
    
    boolean existsByEmail(String email);
    Optional<Client> findByCustomerREf(String customerRef);
    boolean existsByCustomerRef(String customerRef);
    // Find active customers
    @Query("SELECT c FROM Client c WHERE c.status = 'ACTIVE'")
    List<Client> findAllActive();

    


}
