package in.sapphirus.rupee.portfolio.repo;

import in.sapphirus.rupee.portfolio.domain.Holding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HoldingRepository extends JpaRepository<Holding, UUID> {
    List<Holding> findByUserId(UUID userId);
    List<Holding> findByUserIdAndIsPaper(UUID userId, boolean isPaper);
    Optional<Holding> findByUserIdAndSymbolAndProductAndIsPaper(UUID userId, String symbol, String product, boolean isPaper);
    Optional<Holding> findByUserIdAndSymbol(UUID userId, String symbol);

    @Query("SELECT COUNT(h) FROM Holding h WHERE h.userId = :userId AND h.quantity > 0")
    long countOpenPositionsByUserId(@Param("userId") UUID userId);
}