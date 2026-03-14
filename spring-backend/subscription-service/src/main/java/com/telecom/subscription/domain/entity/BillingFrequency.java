package com.telecom.subscription.domain.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.LocalDate;

public enum BillingFrequency {
    MONTHLY(1),
    QUARTERLY(3),
    SEMI_ANNUAL(6),
    ANNUAL(12);
    
    private final int months;
    
    BillingFrequency(int months) {
        this.months = months;
    }
    
    public int getMonths() {
        return months;
    }

    @JsonCreator
    public static BillingFrequency fromString(String value) {
        if (value == null) {
            return BillingFrequency.MONTHLY;
        }
        String normalized = value.trim().toUpperCase();
        return switch (normalized) {
            case "MONTHLY" -> MONTHLY;
            case "QUARTERLY" -> QUARTERLY;
            case "SEMI_ANNUAL", "SEMI-ANNUAL", "SEMIANNUAL" -> SEMI_ANNUAL;
            case "ANNUAL", "YEARLY" -> ANNUAL;
            default -> MONTHLY;
        };
    }
    
    
     //Calculate the next billing date from a given start date
     
    public LocalDate getNextBillingDate(LocalDate fromDate) {
        return fromDate.plusMonths(months);
    }
    
   
     // Calculate billing period end date from start date
     
    public LocalDate getPeriodEnd(LocalDate periodStart) {
        return periodStart.plusMonths(months).minusDays(1);
    }
    
    
     //Check if a given date falls within a billing cycle
     
    public boolean isWithinBillingCycle(LocalDate dateToCheck, LocalDate lastBillingDate) {
        LocalDate nextBillingDate = getNextBillingDate(lastBillingDate);
        return !dateToCheck.isBefore(lastBillingDate) && dateToCheck.isBefore(nextBillingDate);
    }
}
