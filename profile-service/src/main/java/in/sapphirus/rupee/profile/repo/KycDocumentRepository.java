package in.sapphirus.rupee.profile.repo;

import in.sapphirus.rupee.profile.domain.KycDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface KycDocumentRepository extends JpaRepository<KycDocument, UUID> {
    List<KycDocument> findByUserIdOrderByAttemptNumberDesc(UUID userId);
}
