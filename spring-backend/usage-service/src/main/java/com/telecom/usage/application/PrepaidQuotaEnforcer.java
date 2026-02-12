package com.telecom.usage.application;

import com.telecom.usage.domain.entity.UsageType;
import com.telecom.usage.infrastructure.client.CatalogClient;
import com.telecom.usage.infrastructure.client.SubscriptionClient;
import com.telecom.usage.infrastructure.client.SubscriptionClient.QuotaDeductionPayload;
import com.telecom.usage.infrastructure.client.SubscriptionClient.QuotaDeductionResult;
import com.telecom.usage.infrastructure.client.SubscriptionClient.SubscriptionDto;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Enforces prepaid quota rules before usage is recorded.
 *
 * Flow:
 * 1. Fetch subscription → get offreId
 * 2. Fetch offer from catalog → check paymentType
 * 3. If PREPAID → call subscription-service deduct-quota
 * 4. Return result (allowed / rejected)
 *
 * This service does NOT persist anything. It only gates whether
 * the usage should be accepted or rejected.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PrepaidQuotaEnforcer {

    private final SubscriptionClient subscriptionClient;
    private final CatalogClient catalogClient;

    /**
     * Check if usage should be allowed. For postpaid offers, always returns ALLOWED.
     * For prepaid offers, deducts quota and returns the result.
     *
     * @param subscriptionId the subscription consuming usage
     * @param usageType      VOICE, SMS, DATA
     * @param quantity       amount to consume
     * @return enforcement result
     */
    public EnforcementResult enforce(Long subscriptionId, UsageType usageType, BigDecimal quantity) {
        try {
            // 1. Look up the subscription to get offreId
            SubscriptionDto subscription = subscriptionClient.getSubscription(subscriptionId);
            if (subscription == null) {
                return EnforcementResult.rejected("Subscription not found: " + subscriptionId);
            }

            // 2. Look up the offer to check payment type
            CatalogClient.OffreDto offer = catalogClient.getOffreById(subscription.offreId());
            String paymentType = offer.paymentType();

            if (!"PREPAID".equals(paymentType)) {
                // Postpaid — no quota enforcement, always allowed
                log.debug("Postpaid offer for subscription {} — skipping quota check", subscriptionId);
                return EnforcementResult.allowed();
            }

            // 3. Prepaid — deduct quota
            log.info("Prepaid quota check — subscription: {}, type: {}, quantity: {}",
                    subscriptionId, usageType, quantity);

            String quotaType = mapUsageTypeToQuotaType(usageType);
            QuotaDeductionPayload payload = new QuotaDeductionPayload(quotaType, quantity);
            QuotaDeductionResult result = subscriptionClient.deductQuota(subscriptionId, payload);

            if (result.success()) {
                log.info("Quota deducted — subscription: {}, type: {}, remaining: {}",
                        subscriptionId, usageType, result.remaining());
                return EnforcementResult.allowed(result.remaining());
            } else {
                log.warn("Quota insufficient — subscription: {}, type: {}, message: {}",
                        subscriptionId, usageType, result.message());
                return EnforcementResult.rejected(result.message());
            }

        } catch (FeignException.NotFound e) {
            log.error("Subscription or offer not found for subscription: {}", subscriptionId);
            return EnforcementResult.rejected("Subscription or offer not found");
        } catch (FeignException.UnprocessableEntity e) {
            log.warn("Quota deduction rejected for subscription {}: {}", subscriptionId, e.getMessage());
            return EnforcementResult.rejected("Quota deduction failed — insufficient quota");
        } catch (FeignException e) {
            log.error("Service communication error during quota check for subscription {}: {}",
                    subscriptionId, e.getMessage());
            return EnforcementResult.rejected("Service unavailable — cannot verify quota");
        } catch (Exception e) {
            log.error("Unexpected error during quota enforcement for subscription {}", subscriptionId, e);
            return EnforcementResult.rejected("Internal error during quota check");
        }
    }

    private String mapUsageTypeToQuotaType(UsageType usageType) {
        return switch (usageType) {
            case VOICE, VOICE_ROAMING -> "VOICE";
            case SMS, SMS_ROAMING -> "SMS";
            case DATA, DATA_ROAMING -> "DATA";
            case VAS -> "DATA";
        };
    }

    public record EnforcementResult(
            boolean permitted,
            String rejectionReason,
            BigDecimal remainingQuota
    ) {
        public static EnforcementResult allowed() {
            return new EnforcementResult(true, null, null);
        }
        public static EnforcementResult allowed(BigDecimal remainingQuota) {
            return new EnforcementResult(true, null, remainingQuota);
        }
        public static EnforcementResult rejected(String reason) {
            return new EnforcementResult(false, reason, null);
        }
    }
}
