package in.sapphirus.rupee.profile.repo;

import in.sapphirus.rupee.profile.domain.Badge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BadgeRepository extends JpaRepository<Badge, UUID> {
    List<Badge> findByUserId(String userId);
}
