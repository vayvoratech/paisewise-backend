package in.sapphirus.rupee.learn.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "quiz_attempts")
public class QuizAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "lesson_id", nullable = false)
    private String lessonId; // Mapped to Lesson string id e.g. "mf-3"

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber = 1;

    @Column(nullable = false, length = 30)
    private String status = "IN_PROGRESS";

    @Column(name = "questions_served", nullable = false, columnDefinition = "JSONB")
    private String questionsServed = "[]";

    @Column(name = "user_answers", nullable = false, columnDefinition = "JSONB")
    private String userAnswers = "[]";

    @Column(name = "total_questions", nullable = false)
    private int totalQuestions = 0;

    @Column(name = "correct_answers", nullable = false)
    private int correctAnswers = 0;

    @Column(name = "score_pct", nullable = false)
    private double scorePct = 0.0;

    @Column(name = "pass_threshold_pct", nullable = false)
    private double passThresholdPct = 60.0;

    @Column(nullable = false)
    private boolean passed = false;

    @Column(name = "xp_earned", nullable = false)
    private int xpEarned = 0;

    @Column(name = "xp_bonus", nullable = false)
    private int xpBonus = 0;

    @Column(name = "time_spent_seconds", nullable = false)
    private int timeSpentSeconds = 0;

    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt = Instant.now();

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(nullable = false, length = 5)
    private String language = "en";

    protected QuizAttempt() {}

    public QuizAttempt(UUID userId, String lessonId, int attemptNumber) {
        this.userId = userId;
        this.lessonId = lessonId;
        this.attemptNumber = attemptNumber;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getLessonId() { return lessonId; }
    public int getAttemptNumber() { return attemptNumber; }
    public String getStatus() { return status; }
    public String getQuestionsServed() { return questionsServed; }
    public String getUserAnswers() { return userAnswers; }
    public int getTotalQuestions() { return totalQuestions; }
    public int getCorrectAnswers() { return correctAnswers; }
    public double getScorePct() { return scorePct; }
    public double getPassThresholdPct() { return passThresholdPct; }
    public boolean isPassed() { return passed; }
    public int getXpEarned() { return xpEarned; }
    public int getXpBonus() { return xpBonus; }
    public int getTimeSpentSeconds() { return timeSpentSeconds; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getSubmittedAt() { return submittedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public String getLanguage() { return language; }

    public void setStatus(String status) { this.status = status; }
    public void setQuestionsServed(String questions) { this.questionsServed = questions; }
    public void setUserAnswers(String answers) { this.userAnswers = answers; }
    public void setTotalQuestions(int total) { this.totalQuestions = total; }
    public void setCorrectAnswers(int correct) { this.correctAnswers = correct; }
    public void setScorePct(double score) { this.scorePct = score; }
    public void setPassed(boolean passed) { this.passed = passed; }
    public void setXpEarned(int xp) { this.xpEarned = xp; }
    public void setXpBonus(int bonus) { this.xpBonus = bonus; }
    public void setTimeSpentSeconds(int sec) { this.timeSpentSeconds = sec; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public void setLanguage(String lang) { this.language = lang; }
}
