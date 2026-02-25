package com.telecom.customer.domain.repository;

import com.telecom.customer.domain.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    
    Optional<Customer> findByEmail(String email);
    
    boolean existsByEmail(String email);
    Optional<Customer> findByCustomerRef(String customerRef);
    boolean existsByCustomerRef(String customerRef);
    @Query("SELECT c FROM Customer c WHERE c.status = 'ACTIVE'")
    List<Customer> findAllActive();
    
    Optional<Customer> findByPieceIdentite(String pieceIdentite);
    


    


}
