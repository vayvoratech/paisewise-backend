package in.sapphirus.rupee.learn.repo;

import in.sapphirus.rupee.learn.domain.JargonTerm;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JargonRepository extends JpaRepository<JargonTerm, String> {}
