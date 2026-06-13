package in.sapphirus.rupee.learn.api;

import com.fasterxml.jackson.annotation.JsonRawValue;
import in.sapphirus.rupee.learn.domain.JargonTerm;
import in.sapphirus.rupee.learn.domain.Lesson;
import in.sapphirus.rupee.learn.domain.QuizQuestion;
import in.sapphirus.rupee.learn.repo.JargonRepository;
import in.sapphirus.rupee.learn.repo.LessonRepository;
import in.sapphirus.rupee.learn.repo.QuizRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** Read APIs for lessons, jargon dictionary, and the daily quiz. Routed at /learn/**. */
@RestController
@RequestMapping("/learn")
public class LearnController {

    private final LessonRepository lessons;
    private final JargonRepository jargon;
    private final QuizRepository quiz;

    public LearnController(LessonRepository lessons, JargonRepository jargon, QuizRepository quiz) {
        this.lessons = lessons;
        this.jargon = jargon;
        this.quiz = quiz;
    }

    // segments/jargonWords are emitted as raw JSON (already stored as JSON strings).
    public record LessonView(String id, String chapter, int chapterNo, int index, int total,
                             String title, int quizXp,
                             @JsonRawValue String segments, @JsonRawValue String jargonWords) {}

    public record JargonView(String term, String definition, String analogy, String example) {}

    public record QuizView(String id, String prompt, int seconds, int xp,
                           @JsonRawValue String options, String explanation) {}

    @GetMapping("/lessons")
    public List<LessonView> allLessons() {
        return lessons.findAll().stream().map(this::lessonView).toList();
    }

    @GetMapping("/lessons/{id}")
    public LessonView lesson(@PathVariable String id) {
        return lessons.findById(id).map(this::lessonView)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found"));
    }

    @GetMapping("/jargon/{term}")
    public JargonView jargon(@PathVariable String term) {
        JargonTerm t = jargon.findById(term)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Term not found"));
        return new JargonView(t.getTerm(), t.getDefinition(), t.getAnalogy(), t.getExample());
    }

    @GetMapping("/quiz/daily")
    public List<QuizView> dailyQuiz() {
        return quiz.findAllByOrderByOrderNoAsc().stream()
                .map(q -> new QuizView(q.getId(), q.getPrompt(), q.getSeconds(), q.getXp(),
                        q.getOptionsJson(), q.getExplanation()))
                .toList();
    }

    private LessonView lessonView(Lesson l) {
        return new LessonView(l.getId(), l.getChapter(), l.getChapterNo(), l.getIndex(), l.getTotal(),
                l.getTitle(), l.getQuizXp(), l.getSegmentsJson(), l.getJargonWordsJson());
    }
}
