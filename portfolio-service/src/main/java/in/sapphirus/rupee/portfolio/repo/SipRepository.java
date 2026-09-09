package in.sapphirus.rupee.portfolio.repo;

import in.sapphirus.rupee.portfolio.domain.Sip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SipRepository extends JpaRepository<Sip, UUID> {
    List<Sip> findByUserIdOrderByStartDateDesc(UUID userId);
}
