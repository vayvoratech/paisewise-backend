package in.sapphirus.rupee.learn.service;

import in.sapphirus.rupee.learn.domain.Lesson;
import in.sapphirus.rupee.learn.domain.UserLessonProgress;
import in.sapphirus.rupee.learn.domain.JargonTerm;
import in.sapphirus.rupee.learn.repo.LessonRepository;
import in.sapphirus.rupee.learn.repo.UserLessonProgressRepository;
import in.sapphirus.rupee.learn.repo.JargonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Service layer orchestrating financial education lessons progress and glossary terms lookup.
 */
@Service
public class LearnService {

    private final LessonRepository lessonRepo;
    private final UserLessonProgressRepository progressRepo;
    private final JargonRepository jargonRepo;
    private final StreakService streakService;
    private final XpService xpService;

    public LearnService(LessonRepository lessonRepo,
                        UserLessonProgressRepository progressRepo,
                        JargonRepository jargonRepo,
                        StreakService streakService,
                        XpService xpService) {
        this.lessonRepo = lessonRepo;
        this.progressRepo = progressRepo;
        this.jargonRepo = jargonRepo;
        this.streakService = streakService;
        this.xpService = xpService;
    }

    public List<Lesson> getLessons() {
        return lessonRepo.findAll();
    }

    public Lesson getLesson(String id) {
        return lessonRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found: " + id));
    }

    @Transactional
    public void markLessonViewed(UUID userId, String lessonId) {
        UserLessonProgress progress = progressRepo.findByUserIdAndLessonId(userId, lessonId)
                .orElseGet(() -> new UserLessonProgress(userId, lessonId));

        if ("NOT_STARTED".equals(progress.getStatus())) {
            progress.setStatus("IN_PROGRESS");
        }
        progress.setLastViewedAt(Instant.now());
        progressRepo.save(progress);
        streakService.updateStreak(userId);
    }

    @Transactional
    public void completeLesson(UUID userId, String lessonId) {
        UserLessonProgress progress = progressRepo.findByUserIdAndLessonId(userId, lessonId)
                .orElseGet(() -> new UserLessonProgress(userId, lessonId));

        boolean firstTimeComplete = !"COMPLETED".equalsIgnoreCase(progress.getStatus());
        progress.setStatus("COMPLETED");
        progress.setCompletedAt(Instant.now());
        progress.setLastViewedAt(Instant.now());
        progressRepo.save(progress);

        streakService.updateStreak(userId);
        if (firstTimeComplete) {
            xpService.awardXp(userId, 50, "LESSON_COMPLETE_" + lessonId);
        }
    }

    public double getLessonProgress(UUID userId) {
        long total = lessonRepo.count();
        if (total == 0) {
            return 0.0;
        }
        long completed = progressRepo.findByUserId(userId).stream()
                .filter(p -> "COMPLETED".equalsIgnoreCase(p.getStatus()))
                .count();
        return ((double) completed / total) * 100.0;
    }

    public JargonTerm getJargonTerm(String term) {
        return jargonRepo.findById(term)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Jargon term not found: " + term));
    }
}
