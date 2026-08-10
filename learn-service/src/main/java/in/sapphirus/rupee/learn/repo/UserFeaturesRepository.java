package in.sapphirus.rupee.learn.repo;

import in.sapphirus.rupee.learn.domain.UserFeatures;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserFeaturesRepository extends JpaRepository<UserFeatures, UUID> {
}
