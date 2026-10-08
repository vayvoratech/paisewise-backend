package in.sapphirus.rupee.profile.domain;

import jakarta.persistence.*;

/** A user's profile + gamification stats + settings. Keyed by auth userId. */
@Entity
@Table(name = "profiles")
public class Profile {

    @Id
    @Column(name = "user_id")
    private String userId; // matches the JWT subject from auth-service

    private String name;
    private String handle;
    private String city;

    private int level = 1;
    private int dayStreak = 0;
    private int xpTotal = 0;
    private int lessonsCompleted = 0;

    private String language = "Hindi";
    private boolean dailyReminders = true;
    private boolean kycVerified = false;

    protected Profile() {}

    public Profile(String userId, String name, String handle, String city) {
        this.userId = userId;
        this.name = name;
        this.handle = handle;
        this.city = city;
    }

    public String getUserId() { return userId; }
    public String getName() { return name; }
    public String getHandle() { return handle; }
    public String getCity() { return city; }
    public int getLevel() { return level; }
    public int getDayStreak() { return dayStreak; }
    public int getXpTotal() { return xpTotal; }
    public int getLessonsCompleted() { return lessonsCompleted; }
    public String getLanguage() { return language; }
    public boolean isDailyReminders() { return dailyReminders; }
    public boolean isKycVerified() { return kycVerified; }

    public void setName(String name) { this.name = name; }
    public void setHandle(String handle) { this.handle = handle; }
    public void setCity(String city) { this.city = city; }
    public void setLevel(int level) { this.level = level; }
    public void setDayStreak(int dayStreak) { this.dayStreak = dayStreak; }
    public void setXpTotal(int xpTotal) { this.xpTotal = xpTotal; }
    public void setLessonsCompleted(int n) { this.lessonsCompleted = n; }
    public void setLanguage(String language) { this.language = language; }
    public void setDailyReminders(boolean v) { this.dailyReminders = v; }
    public void setKycVerified(boolean v) { this.kycVerified = v; }
}
