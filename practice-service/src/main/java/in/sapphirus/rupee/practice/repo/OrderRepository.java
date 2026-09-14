package in.sapphirus.rupee.practice.repo;

import in.sapphirus.rupee.practice.domain.Order;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    List<Order> findByUserIdOrderByPlacedAtDesc(UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Order> findByIsPaperTrueAndOrderTypeAndStatus(
            String orderType,
            String status
    );
}