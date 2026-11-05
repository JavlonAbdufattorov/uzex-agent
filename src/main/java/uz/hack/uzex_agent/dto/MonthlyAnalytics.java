package uz.hack.uzex_agent.dto;

public record MonthlyAnalytics(
        int year,
        int month,
        long dealsCount,
        double totalLots
) {}