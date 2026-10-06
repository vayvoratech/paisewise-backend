package in.sapphirus.rupee.market.repo;

import in.sapphirus.rupee.market.domain.Watchlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WatchlistRepository extends JpaRepository<Watchlist, UUID> {
    List<Watchlist> findByUserIdOrderBySortOrderAsc(UUID userId);
    Optional<Watchlist> findByUserIdAndSymbol(UUID userId, String symbol);
    int countByUserId(UUID userId);
    void deleteByUserIdAndSymbol(UUID userId, String symbol);
}
