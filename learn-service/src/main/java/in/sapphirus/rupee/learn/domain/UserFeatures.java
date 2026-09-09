package in.sapphirus.rupee.learn.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_features")
public class UserFeatures {

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "lessons_completed_total")
    private int lessonsCompletedTotal = 0;

    @Column(name = "lessons_completed_7d")
    private int lessonsCompleted7d = 0;

    @Column(name = "quiz_attempts_total")
    private int quizAttemptsTotal = 0;

    @Column(name = "quiz_pass_rate")
    private double quizPassRate = 0.0;

    @Column(name = "avg_quiz_score")
    private double avgQuizScore = 0.0;

    @Column(name = "streak_days_current")
    private int streakDaysCurrent = 0;

    @Column(name = "streak_days_max")
    private int streakDaysMax = 0;

    @Column(name = "last_active_at")
    private Instant lastActiveAt = Instant.now();

    @Column(name = "xp_total")
    private int xpTotal = 0;

    @Column(name = "xp_7d")
    private int xp7d = 0;

    @Column(name = "avg_time_spent_seconds")
    private double avgTimeSpentSeconds = 0.0;

    @Column(name = "jargon_taps_total")
    private int jargonTapsTotal = 0;

    @Column(name = "paper_trades_count")
    private int paperTradesCount = 0;

    @Column(name = "paper_profit_loss")
    private double paperProfitLoss = 0.0;

    @Column(name = "forum_posts_count")
    private int forumPostsCount = 0;

    @Column(name = "forum_answers_count")
    private int forumAnswersCount = 0;

    @Column(name = "support_tickets_count")
    private int supportTicketsCount = 0;

    @Column(name = "churn_score")
    private double churnScore = 0.0;

    @Column(name = "churn_risk_level", length = 20)
    private String churnRiskLevel = "LOW";

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected UserFeatures() {}

    public UserFeatures(UUID userId) {
        this.userId = userId;
    }

    public UUID getUserId() { return userId; }
    public int getLessonsCompletedTotal() { return lessonsCompletedTotal; }
    public int getLessonsCompleted7d() { return lessonsCompleted7d; }
    public int getQuizAttemptsTotal() { return quizAttemptsTotal; }
    public double getQuizPassRate() { return quizPassRate; }
    public double getAvgQuizScore() { return avgQuizScore; }
    public int getStreakDaysCurrent() { return streakDaysCurrent; }
    public int getStreakDaysMax() { return streakDaysMax; }
    public Instant getLastActiveAt() { return lastActiveAt; }
    public int getXpTotal() { return xpTotal; }
    public int getXp7d() { return xp7d; }
    public double getAvgTimeSpentSeconds() { return avgTimeSpentSeconds; }
    public int getJargonTapsTotal() { return jargonTapsTotal; }
    public int getPaperTradesCount() { return paperTradesCount; }
    public double getPaperProfitLoss() { return paperProfitLoss; }
    public int getForumPostsCount() { return forumPostsCount; }
    public int getForumAnswersCount() { return forumAnswersCount; }
    public int getSupportTicketsCount() { return supportTicketsCount; }
    public double getChurnScore() { return churnScore; }
    public String getChurnRiskLevel() { return churnRiskLevel; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setLessonsCompletedTotal(int val) { this.lessonsCompletedTotal = val; }
    public void setLessonsCompleted7d(int val) { this.lessonsCompleted7d = val; }
    public void setQuizAttemptsTotal(int val) { this.quizAttemptsTotal = val; }
    public void setQuizPassRate(double val) { this.quizPassRate = val; }
    public void setAvgQuizScore(double val) { this.avgQuizScore = val; }
    public void setStreakDaysCurrent(int val) { this.streakDaysCurrent = val; }
    public void setStreakDaysMax(int val) { this.streakDaysMax = val; }
    public void setLastActiveAt(Instant val) { this.lastActiveAt = val; }
    public void setXpTotal(int val) { this.xpTotal = val; }
    public void setXp7d(int val) { this.xp7d = val; }
    public void setAvgTimeSpentSeconds(double val) { this.avgTimeSpentSeconds = val; }
    public void setJargonTapsTotal(int val) { this.jargonTapsTotal = val; }
    public void setPaperTradesCount(int val) { this.paperTradesCount = val; }
    public void setPaperProfitLoss(double val) { this.paperProfitLoss = val; }
    public void setForumPostsCount(int val) { this.forumPostsCount = val; }
    public void setForumAnswersCount(int val) { this.forumAnswersCount = val; }
    public void setSupportTicketsCount(int val) { this.supportTicketsCount = val; }
    public void setChurnScore(double val) { this.churnScore = val; }
    public void setChurnRiskLevel(String val) { this.churnRiskLevel = val; }
    public void setUpdatedAt(Instant val) { this.updatedAt = val; }
}
