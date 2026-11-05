	
package uz.hack.uzex_agent.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;
import uz.hack.uzex_agent.dto.ProductAnalytics;
import uz.hack.uzex_agent.service.AnalyticsService;
import uz.hack.uzex_agent.dto.AnalyticsOverview;
import uz.hack.uzex_agent.dto.BrokerAnalytics;
import uz.hack.uzex_agent.dto.BuyerAnalytics;
import uz.hack.uzex_agent.dto.MonthlyAnalytics;
import uz.hack.uzex_agent.dto.RegionAnalytics;
import uz.hack.uzex_agent.dto.SellerAnalytics;
import uz.hack.uzex_agent.dto.YearlyAnalytics;


@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;


    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

@GetMapping("/monthly")
public List<MonthlyAnalytics> getMonthlyAnalytics(
        @RequestParam(required = false) Integer year) {

    if (year == null) {
        return analyticsService.getMonthlyAnalytics();
    }

    return analyticsService.getMonthlyAnalyticsByYear(year);
}

@GetMapping("/broker")
public ResponseEntity<List<BrokerAnalytics>> getBrokerAnalytics(
        @RequestParam String query,
        @RequestParam(defaultValue = "10") int limit
) {
    return ResponseEntity.ok(
            analyticsService.getBrokerAnalytics(query, limit)
    );
}

@GetMapping("/company")
public ResponseEntity<?> getCompanyAnalytics(
        @RequestParam String query,
        @RequestParam(defaultValue = "10") int limit
) {
    return ResponseEntity.ok(
            analyticsService.getCompanyAnalytics(query, limit)
    );
}



    @GetMapping("/regions")
    public List<RegionAnalytics> getRegionsAnalytics() {
        return analyticsService.getRegionsAnalytics();
    }
    
    @GetMapping("/top-sellers")
    public List<SellerAnalytics> getTopSellers(
            @RequestParam(defaultValue = "10") int limit) {

        return analyticsService.getTopSellers(limit);
    }

    @GetMapping("/top-buyers")
    public List<BuyerAnalytics> getTopBuyers(
            @RequestParam(defaultValue = "10") int limit) {

        return analyticsService.getTopBuyers(limit);
    }

    @GetMapping("/yearly")
    public List<YearlyAnalytics> getYearlyAnalytics() {
        return analyticsService.getYearlyAnalytics();
    }

    @GetMapping("/top-products")
    public List<ProductAnalytics> getTopProducts(
                @RequestParam(defaultValue = "10") int limit
    ) {
            return analyticsService.getTopProducts(limit);
        }

    @GetMapping("/overview")
    public AnalyticsOverview getOverview() {
        return analyticsService.getOverview();
    }
}

