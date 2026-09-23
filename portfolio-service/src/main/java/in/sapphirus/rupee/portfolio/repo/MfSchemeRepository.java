package in.sapphirus.rupee.portfolio.repo;

import in.sapphirus.rupee.portfolio.domain.MfScheme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MfSchemeRepository extends JpaRepository<MfScheme, String> {
    List<MfScheme> findByCategory(String category);
}
