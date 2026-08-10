package in.sapphirus.rupee.learn.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_lesson_progress", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "lesson_id"})
})
public class UserLessonProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "lesson_id", nullable = false)
    private String lessonId; // Matches Lesson string id e.g. "mf-3"

    @Column(nullable = false, length = 30)
    private String status = "NOT_STARTED";

    @Column(name = "current_block_index", nullable = false)
    private int currentBlockIndex = 0;

    @Column(name = "total_blocks", nullable = false)
    private int totalBlocks = 0;

    @Column(name = "scroll_position_pct", nullable = false)
    private double scrollPositionPct = 0.0;

    @Column(name = "time_spent_seconds", nullable = false)
    private int timeSpentSeconds = 0;

    @Column(name = "jargon_taps", nullable = false)
    private int jargonTaps = 0;

    @Column(nullable = false, length = 5)
    private String language = "en";

    @Column(name = "xp_earned", nullable = false)
    private int xpEarned = 0;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "last_viewed_at", nullable = false)
    private Instant lastViewedAt = Instant.now();

    @Column(name = "started_at", nullable = false)
    private Instant startedAt = Instant.now();

    protected UserLessonProgress() {}

    public UserLessonProgress(UUID userId, String lessonId) {
        this.userId = userId;
        this.lessonId = lessonId;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getLessonId() { return lessonId; }
    public String getStatus() { return status; }
    public int getCurrentBlockIndex() { return currentBlockIndex; }
    public int getTotalBlocks() { return totalBlocks; }
    public double getScrollPositionPct() { return scrollPositionPct; }
    public int getTimeSpentSeconds() { return timeSpentSeconds; }
    public int getJargonTaps() { return jargonTaps; }
    public String getLanguage() { return language; }
    public int getXpEarned() { return xpEarned; }
    public Instant getCompletedAt() { return completedAt; }
    public Instant getLastViewedAt() { return lastViewedAt; }
    public Instant getStartedAt() { return startedAt; }

    public void setStatus(String status) { this.status = status; }
    public void setCurrentBlockIndex(int idx) { this.currentBlockIndex = idx; }
    public void setTotalBlocks(int total) { this.totalBlocks = total; }
    public void setScrollPositionPct(double pct) { this.scrollPositionPct = pct; }
    public void setTimeSpentSeconds(int sec) { this.timeSpentSeconds = sec; }
    public void setJargonTaps(int taps) { this.jargonTaps = taps; }
    public void setLanguage(String lang) { this.language = lang; }
    public void setXpEarned(int xp) { this.xpEarned = xp; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public void setLastViewedAt(Instant lastViewedAt) { this.lastViewedAt = lastViewedAt; }
}
