package uz.hack.uzex_agent.dto;

import java.math.BigDecimal;

public record RegionAnalytics(
        String region,
        long dealsCount,
        BigDecimal totalAmount,
        long uniqueSellers,
        long uniqueBuyers
) {
}