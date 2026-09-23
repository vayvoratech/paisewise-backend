package in.sapphirus.rupee.practice.config;

import in.sapphirus.rupee.practice.domain.Stock;
import in.sapphirus.rupee.practice.repo.StockRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Seeds the practice stock universe (mirrors the mobile app's market.service.ts). */
@Configuration
public class StockSeeder {

    @Bean
    CommandLineRunner seedStocks(StockRepository stocks) {
        return args -> {
            if (stocks.count() > 0) return;
            stocks.save(new Stock("RELIANCE", "Reliance Industries Ltd.", 2952, 1.2, "🛢️",
                    "[2890,2905,2898,2920,2912,2935,2948,2941,2952]"));
            stocks.save(new Stock("TCS", "Tata Consultancy Services", 3801, -0.8, "💻",
                    "[3850,3845,3838,3842,3825,3818,3810,3805,3801]"));
            stocks.save(new Stock("INFY", "Infosys Limited", 1456, -0.6, "🖥️",
                    "[1470,1468,1472,1465,1466,1460,1458,1457,1456]"));
        };
    }
}
