
package uz.hack.uzex_agent.service;

import java.util.List;

import org.springframework.stereotype.Service;
import uz.hack.uzex_agent.dto.RegionAnalytics;
import uz.hack.uzex_agent.dto.SellerAnalytics;
import uz.hack.uzex_agent.dto.YearlyAnalytics;
import uz.hack.uzex_agent.dto.AnalyticsOverview;
import uz.hack.uzex_agent.dto.BrokerAnalytics;
import uz.hack.uzex_agent.dto.BuyerAnalytics;
import uz.hack.uzex_agent.dto.CompanyAnalytics;
import uz.hack.uzex_agent.dto.MonthlyAnalytics;
import uz.hack.uzex_agent.dto.ProductAnalytics;
import uz.hack.uzex_agent.repository.DuckDBRepository;


@Service
public class AnalyticsService {

    private final DuckDBRepository repository;

    public List<MonthlyAnalytics> getMonthlyAnalytics() {
    return repository.getMonthlyAnalytics();
}




public List<CompanyAnalytics> getCompanyAnalytics(
        String query,
        int limit
) {

    if (query == null || query.trim().length() < 2) {
        throw new IllegalArgumentException(
                "Введите название компании или ИНН"
        );
    }

    if (limit < 1 || limit > 100) {
        throw new IllegalArgumentException(
                "Limit must be between 1 and 100"
        );
    }

    return repository.searchCompany(query, limit);
}





public List<BrokerAnalytics> getBrokerAnalytics(String query, int limit) {

    if (query == null || query.trim().length() < 2) {
        throw new IllegalArgumentException(
                "Search query must contain at least 2 characters"
        );
    }

    if (limit < 1 || limit > 100) {
        throw new IllegalArgumentException(
                "Limit must be between 1 and 100"
        );
    }

    return repository.searchBroker(query, limit);
}





    public List<MonthlyAnalytics> getMonthlyAnalyticsByYear(int year) {

    if (year < 2022 || year > 2026) {
        throw new IllegalArgumentException("Invalid year");
    }

    return repository.getMonthlyAnalyticsByYear(year);
}

    public List<BuyerAnalytics> getTopBuyers(int limit) {

    if (limit < 1 || limit > 100) {
        throw new IllegalArgumentException(
            "Limit must be between 1 and 100"
        );
    }

    return repository.getTopBuyers(limit);
}


public List<YearlyAnalytics> getYearlyAnalytics() {
    return repository.getYearlyAnalytics();
}




    public List<RegionAnalytics> getRegionsAnalytics() {
    return repository.getRegionsAnalytics();
    }
    
    
    public AnalyticsService(DuckDBRepository repository) {
        this.repository = repository;
    }
    
    
    public List<ProductAnalytics> getTopProducts(int limit) {

    if (limit < 1 || limit > 100) {
        throw new IllegalArgumentException(
                "Limit must be between 1 and 100"
        );
    }
        return repository.getTopProducts(limit);
    }


    public List<ProductAnalytics> getTopProductsByDeals(int limit) {
    if (limit < 1 || limit > 100) {
        throw new IllegalArgumentException(
                "Limit must be between 1 and 100"
        );
    }

    return repository.getTopProductsByDeals(limit);
}

public List<ProductAnalytics> getTopProductsByAmount(int limit) {
    if (limit < 1 || limit > 100) {
        throw new IllegalArgumentException(
                "Limit must be between 1 and 100"
        );
    }

    return repository.getTopProductsByAmount(limit);
}



     public List<SellerAnalytics> getTopSellers(int limit) {

    if (limit < 1 || limit > 100) {
        throw new IllegalArgumentException(
            "Limit must be between 1 and 100"
        );
    }

    return repository.getTopSellers(limit);
}
    public AnalyticsOverview getOverview() {

        long[] counts = repository.getOverviewCounts();

        return new AnalyticsOverview(
                counts[0],
                counts[1],
                counts[2],
                counts[3]
        );
    }
}

