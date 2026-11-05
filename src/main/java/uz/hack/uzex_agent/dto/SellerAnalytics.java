package uz.hack.uzex_agent.dto;

import java.math.BigDecimal;

public record SellerAnalytics(
        String sellerInn,
        String sellerName,
        long dealsCount,
        BigDecimal totalAmount,
        double totalLots
) {}