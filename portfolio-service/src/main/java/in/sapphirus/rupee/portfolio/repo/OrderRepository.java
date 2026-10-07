package in.sapphirus.rupee.portfolio.repo;

import in.sapphirus.rupee.portfolio.domain.Order;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    List<Order> findByUserIdOrderByPlacedAtDesc(UUID userId);

    Optional<Order> findByBrokerOrderId(String brokerOrderId);

    Optional<Order> findByClientOrderId(String clientOrderId);

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("SELECT o FROM Order o WHERE o.clientOrderId = :clientOrderId")
    Optional<Order> findWithPessimisticReadLockByClientOrderId(@Param("clientOrderId") String clientOrderId);

    @Query(value = "SELECT * FROM orders WHERE client_order_id = :clientOrderId FOR SHARE", nativeQuery = true)
    Optional<Order> findByClientOrderIdForShare(@Param("clientOrderId") String clientOrderId);

    long countByUserIdAndStatusIn(UUID userId, List<String> statuses);
}
