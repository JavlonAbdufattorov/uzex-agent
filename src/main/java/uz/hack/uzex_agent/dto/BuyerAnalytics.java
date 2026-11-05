package uz.hack.uzex_agent.dto;

import java.math.BigDecimal;

public record BuyerAnalytics(
        String buyerInn,
        String buyerName,
        long dealsCount,
        BigDecimal totalAmount,
        double totalLots
) {}