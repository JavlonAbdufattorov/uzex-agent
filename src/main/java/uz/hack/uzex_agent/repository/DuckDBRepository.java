
package uz.hack.uzex_agent.repository;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import uz.hack.uzex_agent.dto.RegionAnalytics;
import uz.hack.uzex_agent.dto.SellerAnalytics;
import uz.hack.uzex_agent.dto.YearlyAnalytics;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import uz.hack.uzex_agent.dto.BrokerAnalytics;
import uz.hack.uzex_agent.dto.BuyerAnalytics;
import uz.hack.uzex_agent.dto.CompanyAnalytics;
import uz.hack.uzex_agent.dto.MonthlyAnalytics;
import uz.hack.uzex_agent.dto.ProductAnalytics;

@Repository
public class DuckDBRepository {

    @Value("${db.path}")
    private String dbPath;


public List<CompanyAnalytics> searchCompany(String query, int limit) {

    String sql = """
        SELECT *
        FROM (
            SELECT
                'BUYER' AS role,
                buyer_inn AS inn,
                buyer_name AS name,
                COUNT(*) AS deals_count,
                SUM(amount) AS total_amount,
                SUM(qty_lots) AS total_lots,
                COUNT(DISTINCT seller_inn) AS counterparties_count
            FROM deals_full
            WHERE CAST(buyer_inn AS VARCHAR) LIKE ?
               OR LOWER(buyer_name) LIKE ?
            GROUP BY buyer_inn, buyer_name

            UNION ALL

            SELECT
                'SELLER' AS role,
                seller_inn AS inn,
                seller_name AS name,
                COUNT(*) AS deals_count,
                SUM(amount) AS total_amount,
                SUM(qty_lots) AS total_lots,
                COUNT(DISTINCT buyer_inn) AS counterparties_count
            FROM deals_full
            WHERE CAST(seller_inn AS VARCHAR) LIKE ?
               OR LOWER(seller_name) LIKE ?
            GROUP BY seller_inn, seller_name
        )
        ORDER BY deals_count DESC, total_amount DESC
        LIMIT ?
        """;

    String search = "%" + query.trim().toLowerCase() + "%";

    List<CompanyAnalytics> companies = new ArrayList<>();

    try (
            Connection connection = getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)
    ) {

        statement.setString(1, search);
        statement.setString(2, search);
        statement.setString(3, search);
        statement.setString(4, search);
        statement.setInt(5, limit);

        try (ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {

                companies.add(new CompanyAnalytics(
                        rs.getString("role"),
                        rs.getString("inn"),
                        rs.getString("name"),
                        rs.getLong("deals_count"),
                        rs.getBigDecimal("total_amount"),
                        rs.getDouble("total_lots"),
                        rs.getLong("counterparties_count")
                ));
            }
        }

        return companies;

    } catch (SQLException e) {
        throw new RuntimeException(
                "Failed to search company analytics", e
        );
    }
}




public List<BrokerAnalytics> searchBroker(String query, int limit) {

    String sql = """
        WITH target_broker AS (
            SELECT
                broker_code,
                inn,
                broker_name,
                region
            FROM brokers
            WHERE
                CAST(broker_code AS VARCHAR) ILIKE ?
                OR CAST(inn AS VARCHAR) LIKE ?
                OR LOWER(broker_name) LIKE ?
            ORDER BY
                CASE
                    WHEN LOWER(broker_name) = ? THEN 0
                    WHEN inn = ? THEN 0
                    WHEN broker_code = ? THEN 0
                    ELSE 1
                END
            LIMIT ?
        ),

        buyer_stats AS (
            SELECT
                buyer_broker AS broker_code,
                COUNT(DISTINCT deal_id) AS deals_count,
                SUM(amount) AS total_amount,
                SUM(qty_lots) AS total_lots,
                COUNT(DISTINCT seller_inn) AS counterparties
            FROM deals
            WHERE buyer_broker IN (
                SELECT broker_code FROM target_broker
            )
            GROUP BY buyer_broker
        ),

        seller_stats AS (
            SELECT
                seller_broker AS broker_code,
                COUNT(DISTINCT deal_id) AS deals_count,
                SUM(amount) AS total_amount,
                SUM(qty_lots) AS total_lots,
                COUNT(DISTINCT buyer_inn) AS counterparties
            FROM deals
            WHERE seller_broker IN (
                SELECT broker_code FROM target_broker
            )
            GROUP BY seller_broker
        ),

        all_broker_deals AS (
            SELECT buyer_broker AS broker_code, deal_id
            FROM deals
            WHERE buyer_broker IN (
                SELECT broker_code FROM target_broker
            )

            UNION

            SELECT seller_broker AS broker_code, deal_id
            FROM deals
            WHERE seller_broker IN (
                SELECT broker_code FROM target_broker
            )
        ),

        total_stats AS (
            SELECT
                bd.broker_code,
                COUNT(*) AS deals_count,
                SUM(d.amount) AS total_amount,
                SUM(d.qty_lots) AS total_lots
            FROM all_broker_deals bd
            JOIN deals d ON d.deal_id = bd.deal_id
            GROUP BY bd.broker_code
        ),

        all_counterparties AS (
            SELECT
                buyer_broker AS broker_code,
                seller_inn AS counterparty_inn
            FROM deals
            WHERE buyer_broker IN (
                SELECT broker_code FROM target_broker
            )

            UNION

            SELECT
                seller_broker AS broker_code,
                buyer_inn AS counterparty_inn
            FROM deals
            WHERE seller_broker IN (
                SELECT broker_code FROM target_broker
            )
        ),

        counterparty_stats AS (
            SELECT
                broker_code,
                COUNT(DISTINCT counterparty_inn) AS counterparties
            FROM all_counterparties
            GROUP BY broker_code
        )

        SELECT
            b.broker_code,
            b.inn,
            b.broker_name AS name,
            b.region,

            COALESCE(bs.deals_count, 0) AS buyer_deals,
            COALESCE(bs.total_amount, 0) AS buyer_amount,
            COALESCE(bs.total_lots, 0) AS buyer_lots,
            COALESCE(bs.counterparties, 0) AS buyer_counterparties,

            COALESCE(ss.deals_count, 0) AS seller_deals,
            COALESCE(ss.total_amount, 0) AS seller_amount,
            COALESCE(ss.total_lots, 0) AS seller_lots,
            COALESCE(ss.counterparties, 0) AS seller_counterparties,

            COALESCE(ts.deals_count, 0) AS unique_deals,
            COALESCE(ts.total_amount, 0) AS unique_amount,
            COALESCE(ts.total_lots, 0) AS unique_lots,
            COALESCE(cs.counterparties, 0) AS unique_counterparties

        FROM target_broker b
        LEFT JOIN buyer_stats bs
            ON bs.broker_code = b.broker_code
        LEFT JOIN seller_stats ss
            ON ss.broker_code = b.broker_code
        LEFT JOIN total_stats ts
            ON ts.broker_code = b.broker_code
        LEFT JOIN counterparty_stats cs
            ON cs.broker_code = b.broker_code

        ORDER BY unique_deals DESC
        """;

    String normalized = query.trim().toLowerCase();
    String search = "%" + normalized + "%";

    List<BrokerAnalytics> result = new ArrayList<>();

    try (
            Connection connection = getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)
    ) {
        statement.setString(1, search);
        statement.setString(2, search);
        statement.setString(3, search);
        statement.setString(4, normalized);
        statement.setString(5, query.trim());
        statement.setString(6, query.trim());
        statement.setInt(7, limit);

        try (ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                result.add(new BrokerAnalytics(
                        rs.getString("broker_code"),
                        rs.getString("inn"),
                        rs.getString("name"),
                        rs.getString("region"),

                        rs.getLong("buyer_deals"),
                        rs.getBigDecimal("buyer_amount"),
                        rs.getDouble("buyer_lots"),
                        rs.getLong("buyer_counterparties"),

                        rs.getLong("seller_deals"),
                        rs.getBigDecimal("seller_amount"),
                        rs.getDouble("seller_lots"),
                        rs.getLong("seller_counterparties"),

                        rs.getLong("unique_deals"),
                        rs.getBigDecimal("unique_amount"),
                        rs.getDouble("unique_lots"),
                        rs.getLong("unique_counterparties")
                ));
            }
        }

        return result;

    } catch (SQLException e) {
        throw new RuntimeException(
                "Failed to retrieve broker analytics", e
        );
    }
}




public List<MonthlyAnalytics> getMonthlyAnalyticsByYear(int year) {

    String sql = """
        SELECT
            EXTRACT(YEAR FROM deal_date) AS year,
            EXTRACT(MONTH FROM deal_date) AS month,
            COUNT(*) AS deals_count,
            SUM(qty_lots) AS total_lots
        FROM deals_full
        WHERE EXTRACT(YEAR FROM deal_date) = ?
        GROUP BY year, month
        ORDER BY year, month
        """;

    List<MonthlyAnalytics> result = new ArrayList<>();

    try (Connection connection = getConnection();
         PreparedStatement statement = connection.prepareStatement(sql)) {

        statement.setInt(1, year);

        try (ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                result.add(new MonthlyAnalytics(
                    rs.getInt("year"),
                    rs.getInt("month"),
                    rs.getLong("deals_count"),
                    rs.getDouble("total_lots")
                ));
            }
        }

        return result;

    } catch (SQLException e) {
        throw new RuntimeException(
            "Failed to retrieve monthly analytics", e
        );
    }
}

// Топ товаров по количеству сделок
public List<ProductAnalytics> getTopProductsByDeals(int limit) {

    String sql = """
            SELECT
                product_name,
                COUNT(*) AS deals_count,
                SUM(amount) AS total_amount,
                SUM(qty_lots) AS total_lots
            FROM deals_full
            GROUP BY product_name
            ORDER BY deals_count DESC, total_amount DESC
            LIMIT ?
            """;

    List<ProductAnalytics> products = new ArrayList<>();

    try (
            Connection connection = getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)
    ) {
        statement.setInt(1, limit);

        try (ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                products.add(new ProductAnalytics(
                        rs.getString("product_name"),
                        rs.getLong("deals_count"),
                        rs.getBigDecimal("total_amount"),
                        rs.getDouble("total_lots")
                ));
            }
        }

        return products;

    } catch (SQLException e) {
        throw new RuntimeException(
                "Failed to retrieve top products by deals", e
        );
    }
}


// Топ товаров по денежному обороту
public List<ProductAnalytics> getTopProductsByAmount(int limit) {

    String sql = """
            SELECT
                product_name,
                COUNT(*) AS deals_count,
                SUM(amount) AS total_amount,
                SUM(qty_lots) AS total_lots
            FROM deals_full
            GROUP BY product_name
            ORDER BY total_amount DESC, deals_count DESC
            LIMIT ?
            """;

    List<ProductAnalytics> products = new ArrayList<>();

    try (
            Connection connection = getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)
    ) {
        statement.setInt(1, limit);

        try (ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                products.add(new ProductAnalytics(
                        rs.getString("product_name"),
                        rs.getLong("deals_count"),
                        rs.getBigDecimal("total_amount"),
                        rs.getDouble("total_lots")
                ));
            }
        }

        return products;

    } catch (SQLException e) {
        throw new RuntimeException(
                "Failed to retrieve top products by amount", e
        );
    }
}   

public List<MonthlyAnalytics> getMonthlyAnalytics() {

    String sql = """
        SELECT
            EXTRACT(YEAR FROM deal_date) AS year,
            EXTRACT(MONTH FROM deal_date) AS month,
            COUNT(*) AS deals_count,
            SUM(qty_lots) AS total_lots
        FROM deals_full
        GROUP BY year, month
        ORDER BY year, month
        """;

    List<MonthlyAnalytics> result = new ArrayList<>();

    try (Connection connection = getConnection();
         Statement statement = connection.createStatement();
         ResultSet rs = statement.executeQuery(sql)) {

        while (rs.next()) {
            result.add(new MonthlyAnalytics(
                rs.getInt("year"),
                rs.getInt("month"),
                rs.getLong("deals_count"),
                rs.getDouble("total_lots")
            ));
        }

        return result;

    } catch (SQLException e) {
        throw new RuntimeException(
            "Failed to retrieve monthly analytics", e
        );
    }
}



public List<YearlyAnalytics> getYearlyAnalytics() {

    String sql = """
        SELECT
            EXTRACT(YEAR FROM deal_date) AS year,
            COUNT(*) AS deals_count,
            SUM(qty_lots) AS total_lots
        FROM deals_full
        GROUP BY year
        ORDER BY year
        """;

    List<YearlyAnalytics> result = new ArrayList<>();

    try (Connection connection = getConnection();
         Statement statement = connection.createStatement();
         ResultSet rs = statement.executeQuery(sql)) {

        while (rs.next()) {
            result.add(new YearlyAnalytics(
                rs.getInt("year"),
                rs.getLong("deals_count"),
                rs.getDouble("total_lots")
            ));
        }

        return result;

    } catch (SQLException e) {
        throw new RuntimeException(
            "Failed to retrieve yearly analytics", e
        );
    }
}



public List<BuyerAnalytics> getTopBuyers(int limit) {

    String sql = """
        SELECT
            buyer_inn,
            buyer_name,
            COUNT(*) AS deals_count,
            SUM(amount) AS total_amount,
            SUM(qty_lots) AS total_lots
        FROM deals_full
        GROUP BY buyer_inn, buyer_name
        ORDER BY deals_count DESC
        LIMIT ?
        """;

    List<BuyerAnalytics> buyers = new ArrayList<>();

    try (Connection connection = getConnection();
         PreparedStatement statement = connection.prepareStatement(sql)) {

        statement.setInt(1, limit);

        try (ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                buyers.add(new BuyerAnalytics(
                    rs.getString("buyer_inn"),
                    rs.getString("buyer_name"),
                    rs.getLong("deals_count"),
                    rs.getBigDecimal("total_amount"),
                    rs.getDouble("total_lots")
                ));
            }
        }

        return buyers;

    } catch (SQLException e) {
        throw new RuntimeException("Failed to retrieve top buyers", e);
    }
}




public List<SellerAnalytics> getTopSellers(int limit) {

    String sql = """
        SELECT
            seller_inn,
            seller_name,
            COUNT(*) AS deals_count,
            SUM(amount) AS total_amount,
            SUM(qty_lots) AS total_lots
        FROM deals_full
        GROUP BY seller_inn, seller_name
        ORDER BY deals_count DESC
        LIMIT ?
        """;

    List<SellerAnalytics> sellers = new ArrayList<>();

    try (Connection connection = getConnection();
         PreparedStatement statement = connection.prepareStatement(sql)) {

        statement.setInt(1, limit);

        try (ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {
                sellers.add(new SellerAnalytics(
                    rs.getString("seller_inn"),
                    rs.getString("seller_name"),
                    rs.getLong("deals_count"),
                    rs.getBigDecimal("total_amount"),
                    rs.getDouble("total_lots")
                ));
            }
        }

        return sellers;

    } catch (SQLException e) {
        throw new RuntimeException("Failed to retrieve top sellers", e);
    }
}



public List<ProductAnalytics> getTopProducts(int limit) {

    String sql = """
            SELECT
                product_name,
                COUNT(*) AS deals_count,
                SUM(amount) AS total_amount,
                SUM(qty_lots) AS total_lots
            FROM deals_full
            GROUP BY product_name
            ORDER BY total_amount DESC
            LIMIT ?
            """;

    List<ProductAnalytics> products = new ArrayList<>();

    try (
            Connection connection = getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)
    ) {

        statement.setInt(1, limit);

        try (ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {

                products.add(new ProductAnalytics(
                        rs.getString("product_name"),
                        rs.getLong("deals_count"),
                        rs.getBigDecimal("total_amount"),
                        rs.getDouble("total_lots")
                ));
            }
        }

        return products;

    } catch (SQLException e) {
        throw new RuntimeException("Failed to retrieve top products", e);
    }
}


public List<RegionAnalytics> getRegionsAnalytics() {

    String sql = """
            SELECT
                region,
                COUNT(*) AS deals_count,
                SUM(amount) AS total_amount,
                COUNT(DISTINCT seller_inn) AS unique_sellers,
                COUNT(DISTINCT buyer_inn) AS unique_buyers
            FROM deals_full
            GROUP BY region
            ORDER BY deals_count DESC
            """;

    List<RegionAnalytics> regions = new ArrayList<>();

    try (
            Connection connection = getConnection();
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery(sql)
    ) {

        while (rs.next()) {

            regions.add(new RegionAnalytics(
                    rs.getString("region"),
                    rs.getLong("deals_count"),
                    rs.getBigDecimal("total_amount"),
                    rs.getLong("unique_sellers"),
                    rs.getLong("unique_buyers")
            ));
        }

        return regions;

    } catch (SQLException e) {
        throw new RuntimeException(
                "Failed to retrieve regions analytics", e
        );
    }
}

    public Connection getConnection() throws SQLException {

        Properties props = new Properties();
        props.setProperty("duckdb.read_only", "true");

        return DriverManager.getConnection(
                "jdbc:duckdb:" + dbPath,
                props
        );
    }

    public long[] getOverviewCounts() {

        String sql = """
                SELECT
                    (SELECT COUNT(*) FROM deals),
                    (SELECT COUNT(*) FROM contracts),
                    (SELECT COUNT(*) FROM participants),
                    (SELECT COUNT(*) FROM brokers)
                """;

        try (
                Connection connection = getConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(sql)
        ) {

            resultSet.next();

            return new long[]{
                    resultSet.getLong(1),
                    resultSet.getLong(2),
                    resultSet.getLong(3),
                    resultSet.getLong(4)
            };

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to retrieve analytics from DuckDB", e
            );
        }
    }
}
