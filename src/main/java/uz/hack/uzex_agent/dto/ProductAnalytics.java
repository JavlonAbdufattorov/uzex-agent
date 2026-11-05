
package uz.hack.uzex_agent.dto;

import java.math.BigDecimal;

public record ProductAnalytics(
        String productName,
        long dealsCount,
        BigDecimal totalAmount,
        double totalLots
) {
}
