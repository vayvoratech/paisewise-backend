package in.sapphirus.rupee.practice.repo;

import in.sapphirus.rupee.practice.domain.Trade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TradeRepository extends JpaRepository<Trade, UUID> {
}