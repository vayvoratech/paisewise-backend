package in.sapphirus.rupee.learn.repo;

import in.sapphirus.rupee.learn.domain.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LessonRepository extends JpaRepository<Lesson, String> {}
