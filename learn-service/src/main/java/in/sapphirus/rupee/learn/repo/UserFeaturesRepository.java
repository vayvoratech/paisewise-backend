package in.sapphirus.rupee.learn.repo;

import in.sapphirus.rupee.learn.domain.UserFeatures;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface UserFeaturesRepository extends JpaRepository<UserFeatures, UUID> {

    @Modifying
    @Query("UPDATE UserFeatures uf SET uf.streakDaysCurrent = 0 WHERE uf.lastActiveAt < :cutoff")
    int resetStreaksOlderThan(@Param("cutoff") Instant cutoff);
}
