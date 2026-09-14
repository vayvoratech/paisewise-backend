package in.sapphirus.rupee.practice.repo;

import in.sapphirus.rupee.practice.domain.PaperAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PaperAccountRepository extends JpaRepository<PaperAccount, UUID> {
}