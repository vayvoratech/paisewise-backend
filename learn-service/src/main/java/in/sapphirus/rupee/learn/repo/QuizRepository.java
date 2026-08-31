package in.sapphirus.rupee.learn.repo;

import in.sapphirus.rupee.learn.domain.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizRepository extends JpaRepository<QuizQuestion, String> {
    List<QuizQuestion> findAllByOrderByOrderNoAsc();
}
