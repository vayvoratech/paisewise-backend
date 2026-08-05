package in.sapphirus.rupee.practice.repo;

import in.sapphirus.rupee.practice.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    List<Order> findByUserIdOrderByPlacedAtDesc(UUID userId);
}