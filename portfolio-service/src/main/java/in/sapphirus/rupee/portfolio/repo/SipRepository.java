package in.sapphirus.rupee.portfolio.repo;

import in.sapphirus.rupee.portfolio.domain.Sip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SipRepository extends JpaRepository<Sip, UUID> {

    List<Sip> findByUserIdOrderByStartDateDesc(UUID userId);

    /** Resolve a Razorpay UPI mandate webhook to a SIP. */
    Optional<Sip> findByUpiMandateId(String upiMandateId);

    /** Resolve a Razorpay Subscription webhook to a SIP. */
    Optional<Sip> findByRazorpaySubscriptionId(String razorpaySubscriptionId);

    /** Used by the scheduled debit job to find SIPs due for processing. */
    List<Sip> findByStatusAndNextDebitDateLessThanEqual(String status, LocalDate dueDate);
}

