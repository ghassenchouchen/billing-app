package com.telecom.boutique.domain.repository;

import com.telecom.boutique.domain.entity.Boutique;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BoutiqueRepository extends JpaRepository<Boutique, Long> {
    Optional<Boutique> findByCode(String code);
    boolean existsByCode(String code);
}
