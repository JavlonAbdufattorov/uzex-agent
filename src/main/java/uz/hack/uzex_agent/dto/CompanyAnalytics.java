package uz.hack.uzex_agent.dto;

import java.math.BigDecimal;

public record CompanyAnalytics(
        String role,
        String inn,
        String name,
        long dealsCount,
        BigDecimal totalAmount,
        double totalLots,
        long counterpartiesCount
) {}