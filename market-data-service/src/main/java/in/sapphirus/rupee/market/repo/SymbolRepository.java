package in.sapphirus.rupee.market.repo;

import in.sapphirus.rupee.market.domain.Symbol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SymbolRepository extends JpaRepository<Symbol, String> {

    List<Symbol> findBySymbolIn(List<String> symbols);

    List<Symbol> findByInstrumentType(String instrumentType);

    @Query(value = "SELECT * FROM symbols " +
                   "WHERE similarity(company_name, :query) > 0.15 " +
                   "   OR similarity(short_name, :query) > 0.15 " +
                   "   OR symbol ILIKE :queryEscaped " +
                   "ORDER BY similarity(company_name, :query) DESC " +
                   "LIMIT :lim", nativeQuery = true)
    List<Symbol> searchFuzzy(@Param("query") String query, 
                             @Param("queryEscaped") String queryEscaped, 
                             @Param("lim") int lim);
}
