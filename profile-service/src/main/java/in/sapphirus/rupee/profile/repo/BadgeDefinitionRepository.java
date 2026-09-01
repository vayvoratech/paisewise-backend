package in.sapphirus.rupee.profile.repo;

import in.sapphirus.rupee.profile.domain.BadgeDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BadgeDefinitionRepository extends JpaRepository<BadgeDefinition, String> {
    List<BadgeDefinition> findByCategoryOrderBySortOrderAsc(String category);
}
