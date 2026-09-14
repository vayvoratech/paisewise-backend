package in.sapphirus.rupee.learn.repo;

import in.sapphirus.rupee.learn.domain.JargonTerm;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface JargonRepository extends JpaRepository<JargonTerm, String> {
    Optional<JargonTerm> findByTermIgnoreCase(String term);
    Optional<JargonTerm> findFirstByTermContainingIgnoreCase(String term);
}
