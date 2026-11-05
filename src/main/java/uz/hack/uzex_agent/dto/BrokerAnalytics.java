package uz.hack.uzex_agent.dto;

import java.math.BigDecimal;

public record BrokerAnalytics(
        String brokerCode,
        String inn,
        String name,
        String region,

        long buyerDealsCount,
        BigDecimal buyerTotalAmount,
        double buyerTotalLots,
        long buyerCounterpartiesCount,

        long sellerDealsCount,
        BigDecimal sellerTotalAmount,
        double sellerTotalLots,
        long sellerCounterpartiesCount,

        long totalUniqueDeals,
        BigDecimal totalUniqueAmount,
        double totalUniqueLots,
        long totalUniqueCounterparties
) {}