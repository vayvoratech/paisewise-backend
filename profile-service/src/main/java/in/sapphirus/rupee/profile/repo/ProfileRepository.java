package in.sapphirus.rupee.profile.repo;

import in.sapphirus.rupee.profile.domain.Profile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfileRepository extends JpaRepository<Profile, String> {}
