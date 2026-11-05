package uz.hack.uzex_agent;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;

@RestController
public class PingController {

    @Value("${db.path}")
    private String dbPath;

    @GetMapping("/ping")
    public String ping() throws Exception { 
        Properties props = new Properties();
        props.setProperty("duckdb.read_only", "true");
        try (Connection c = DriverManager.getConnection("jdbc:duckdb:" + dbPath, props);
            Statement st = c.createStatement();
            ResultSet rs = st.executeQuery("SELECT count(*) FROM deals")) {
            rs.next();
            return "deals = " + rs.getLong(1);
        }
    }
}