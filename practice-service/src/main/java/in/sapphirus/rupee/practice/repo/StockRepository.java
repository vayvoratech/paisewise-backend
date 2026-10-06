package in.sapphirus.rupee.practice.repo;

import in.sapphirus.rupee.practice.domain.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockRepository extends JpaRepository<Stock, String> {}
