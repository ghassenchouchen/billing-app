package com.telecom.subscription.application;

import com.telecom.subscription.domain.entity.Abonnement;
import com.telecom.subscription.domain.entity.Quota;
import com.telecom.subscription.domain.repository.AbonnementRepository;
import com.telecom.subscription.domain.repository.QuotaRepository;
import com.telecom.subscription.web.dto.QuotaDeductionRequest;
import com.telecom.subscription.web.dto.QuotaDeductionResponse;
import com.telecom.subscription.web.dto.QuotaDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuotaService {

    private final QuotaRepository quotaRepository;
    private final AbonnementRepository abonnementRepository;

    @Transactional
    public QuotaDeductionResponse deductQuota(Long subscriptionId, QuotaDeductionRequest request) {
        Abonnement abonnement = abonnementRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found: " + subscriptionId));

        if (!abonnement.isActive()) {
            throw new IllegalStateException("Subscription " + subscriptionId + " is not active (status: " + abonnement.getStatus() + ")");
        }

        var quota = quotaRepository.findByAbonnementIdAndQuotaTypeForUpdate(
                subscriptionId, request.usageType());

        if (quota.isEmpty()) {
            log.warn("No {} quota configured for subscription {}", request.usageType(), subscriptionId);
            return QuotaDeductionResponse.noQuotaConfigured(request.usageType(), subscriptionId);
        }

        Quota q = quota.get();

        if (!q.deduct(request.quantity())) {
            log.info("Insufficient {} quota for subscription {} — remaining: {}, requested: {}",
                    request.usageType(), subscriptionId, q.getRemainingAmount(), request.quantity());
            return QuotaDeductionResponse.insufficientQuota(q, request.quantity());
        }

        quotaRepository.save(q);

        log.info("Deducted {} {} from subscription {} — remaining: {}",
                request.quantity(), request.usageType(), subscriptionId, q.getRemainingAmount());

        return QuotaDeductionResponse.success(q, request.quantity());
    }

    @Transactional(readOnly = true)
    public List<QuotaDto> getQuotasBySubscription(Long subscriptionId) {
        return quotaRepository.findByAbonnementId(subscriptionId).stream()
                .map(QuotaDto::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public QuotaDto getQuotaByType(Long subscriptionId, Quota.QuotaType quotaType) {
        return quotaRepository.findByAbonnementIdAndQuotaType(subscriptionId, quotaType)
                .map(QuotaDto::fromEntity)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No " + quotaType + " quota for subscription " + subscriptionId));
    }

    @Transactional
    public List<QuotaDto> initializeQuotas(Long subscriptionId, List<QuotaInitRequest> quotas) {
        Abonnement abonnement = abonnementRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found: " + subscriptionId));

        return quotas.stream().map(req -> {
            Quota quota = quotaRepository.findByAbonnementIdAndQuotaType(subscriptionId, req.quotaType())
                    .orElse(Quota.builder()
                            .abonnement(abonnement)
                            .quotaType(req.quotaType())
                            .build());

            quota.setTotalAmount(req.totalAmount());
            quota.setRemainingAmount(req.totalAmount());
            quota.setUnit(req.unit());

            quota = quotaRepository.save(quota);
            log.info("Initialized {} quota for subscription {} — total: {} {}",
                    req.quotaType(), subscriptionId, req.totalAmount(), req.unit());

            return QuotaDto.fromEntity(quota);
        }).toList();
    }

    @Transactional
    public List<QuotaDto> resetQuotas(Long subscriptionId) {
        List<Quota> quotas = quotaRepository.findByAbonnementId(subscriptionId);
        if (quotas.isEmpty()) {
            throw new IllegalArgumentException("No quotas found for subscription " + subscriptionId);
        }

        quotas.forEach(q -> q.setRemainingAmount(q.getTotalAmount()));
        quotaRepository.saveAll(quotas);

        log.info("Reset all quotas for subscription {}", subscriptionId);
        return quotas.stream().map(QuotaDto::fromEntity).toList();
    }

    public record QuotaInitRequest(
            Quota.QuotaType quotaType,
            BigDecimal totalAmount,
            String unit
    ) {}
}
