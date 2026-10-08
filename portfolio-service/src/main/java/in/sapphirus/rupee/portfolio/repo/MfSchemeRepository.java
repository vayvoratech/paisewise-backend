package in.sapphirus.rupee.portfolio.repo;

import in.sapphirus.rupee.portfolio.domain.MfScheme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MfSchemeRepository extends JpaRepository<MfScheme, String>, JpaSpecificationExecutor<MfScheme> {

    List<MfScheme> findByCategoryIgnoreCaseAndIsActiveTrue(String category);

    List<MfScheme> findByRiskLevelIgnoreCaseAndIsActiveTrue(String riskLevel);

    List<MfScheme> findByIsTaxSaverTrueAndIsActiveTrue();

    List<MfScheme> findByIsActiveTrueOrderByReturns3yDesc();

    List<MfScheme> findTop10ByIsActiveTrueOrderByReturns3yDesc();

    List<MfScheme> findTop10ByIsActiveTrueOrderByReturns1yDesc();

    Optional<MfScheme> findByIsin(String isin);

    @Query("SELECT DISTINCT s.category FROM MfScheme s WHERE s.category IS NOT NULL AND s.isActive = true ORDER BY s.category ASC")
    List<String> findDistinctCategories();

    @Query("SELECT DISTINCT s.amcName FROM MfScheme s WHERE s.amcName IS NOT NULL AND s.isActive = true ORDER BY s.amcName ASC")
    List<String> findDistinctAmcs();

    @Query("SELECT s FROM MfScheme s WHERE s.isActive = true AND " +
           "(LOWER(s.schemeName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(s.amcName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(s.category) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(COALESCE(s.subCategory, '')) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(s.schemeCode) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<MfScheme> searchSchemes(@Param("query") String query);
}

