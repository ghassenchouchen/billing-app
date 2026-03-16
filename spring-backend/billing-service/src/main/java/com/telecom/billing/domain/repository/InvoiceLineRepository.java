package com.telecom.billing.domain.repository;

import com.telecom.billing.domain.entity.InvoiceLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoiceLineRepository extends JpaRepository<InvoiceLine, Long> {
    
    List<InvoiceLine> findByFactureId(Long factureId);
    
    List<InvoiceLine> findByServiceId(Long serviceId);
    
    List<InvoiceLine> findByUsageId(Long usageId);
    
    void deleteByFactureId(Long factureId);
}
