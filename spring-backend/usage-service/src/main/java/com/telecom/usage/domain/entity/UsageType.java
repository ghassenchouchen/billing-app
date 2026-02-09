package com.telecom.usage.domain.entity;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Represents the type of telecommunications usage/CDR
 */
@Schema(description = "Type of telecommunications usage")
public enum UsageType {
    
    @Schema(description = "Voice call usage - measured in seconds")
    VOICE,
    
    @Schema(description = "SMS message usage - measured in count")
    SMS,
    
    @Schema(description = "Data usage - measured in bytes/KB/MB/GB")
    DATA,
    
    @Schema(description = "Roaming voice calls")
    VOICE_ROAMING,
    
    @Schema(description = "Roaming data usage")
    DATA_ROAMING,
    
    @Schema(description = "Roaming SMS")
    SMS_ROAMING,
    
    @Schema(description = "Value-added services")
    VAS
}
