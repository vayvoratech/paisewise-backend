package in.sapphirus.rupee.profile.repo;

import in.sapphirus.rupee.profile.domain.Profile;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface ProfileRepository extends JpaRepository<Profile, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Profile p where p.userId = :userId")
    Optional<Profile> findAndLockByUserId(String userId);
}
