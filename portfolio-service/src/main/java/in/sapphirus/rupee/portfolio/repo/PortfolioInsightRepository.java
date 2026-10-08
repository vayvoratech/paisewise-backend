package in.sapphirus.rupee.portfolio.repo;

import in.sapphirus.rupee.portfolio.domain.PortfolioInsight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PortfolioInsightRepository extends JpaRepository<PortfolioInsight, UUID> {
    List<PortfolioInsight> findByUserIdOrderByInsightDateDesc(UUID userId);
}
