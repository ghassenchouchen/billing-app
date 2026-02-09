package com.telecom.catalog.domain.repository;

import com.telecom.catalog.domain.entity.Offre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface OffreRepository extends JpaRepository<Offre, Long> {
    
    Optional<Offre> findByCode(String code);
    
    List<Offre> findByStatus(Offre.OffreStatus status);
}
