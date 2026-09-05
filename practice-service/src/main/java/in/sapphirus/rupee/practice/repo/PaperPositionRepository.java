package in.sapphirus.rupee.practice.repo;

import in.sapphirus.rupee.practice.domain.PaperPosition;
import in.sapphirus.rupee.practice.domain.PaperPositionId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaperPositionRepository
        extends JpaRepository<PaperPosition, PaperPositionId> {
    Optional<PaperPosition> findByUserIdAndSymbol(UUID userId, String symbol);
}