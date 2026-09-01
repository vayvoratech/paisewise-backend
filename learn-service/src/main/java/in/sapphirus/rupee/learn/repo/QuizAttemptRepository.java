package in.sapphirus.rupee.learn.repo;

import in.sapphirus.rupee.learn.domain.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, UUID> {
    List<QuizAttempt> findByUserIdOrderByStartedAtDesc(UUID userId);
    List<QuizAttempt> findByUserIdAndLessonIdOrderByAttemptNumberDesc(UUID userId, String lessonId);
}
