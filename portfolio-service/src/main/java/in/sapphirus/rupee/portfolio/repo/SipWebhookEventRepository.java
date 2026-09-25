package in.sapphirus.rupee.portfolio.repo;

import in.sapphirus.rupee.portfolio.domain.SipWebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SipWebhookEventRepository extends JpaRepository<SipWebhookEvent, UUID> {

    /** Used for idempotency check before the DB UNIQUE constraint fires. */
    boolean existsByEventId(String eventId);

    /** Used by preview dashboard to show recent audit log. */
    java.util.List<SipWebhookEvent> findTop20ByOrderByProcessedAtDesc();
}
