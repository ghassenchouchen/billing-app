package com.telecom.catalog.domain.repository;

import com.telecom.catalog.domain.entity.ServiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface ServiceRepository extends JpaRepository<ServiceEntity, Long> {
    
    Optional<ServiceEntity> findByCode(String code);
    
    List<ServiceEntity> findByActiveTrue();
    
    List<ServiceEntity> findByCategory(ServiceEntity.ServiceCategory category);
}
