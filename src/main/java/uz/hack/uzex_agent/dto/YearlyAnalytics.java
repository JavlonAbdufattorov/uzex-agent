package uz.hack.uzex_agent.dto;

public record YearlyAnalytics(
        int year,
        long dealsCount,
        double totalLots
) {}