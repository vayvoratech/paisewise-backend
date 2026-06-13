package in.sapphirus.rupee.profile.api;

import in.sapphirus.rupee.profile.domain.Badge;
import in.sapphirus.rupee.profile.domain.Profile;
import in.sapphirus.rupee.profile.repo.BadgeRepository;
import in.sapphirus.rupee.profile.repo.ProfileRepository;
import in.sapphirus.rupee.security.CurrentUser;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Profile + badges + settings for the authenticated user. Routed at /profile/**. */
@RestController
@RequestMapping("/profile")
public class ProfileController {

    private final ProfileRepository profiles;
    private final BadgeRepository badges;

    public ProfileController(ProfileRepository profiles, BadgeRepository badges) {
        this.profiles = profiles;
        this.badges = badges;
    }

    public record ProfileView(String userId, String name, String handle, String city, int level,
                              int dayStreak, int xpTotal, int lessonsCompleted, String language,
                              boolean dailyReminders, boolean kycVerified) {}

    public record BadgeView(String emoji, String title, String category) {}

    public record SettingsUpdate(String language, Boolean dailyReminders) {}

    @GetMapping("/me")
    public ProfileView me() {
        return view(getOrCreate());
    }

    @GetMapping("/me/badges")
    public List<BadgeView> myBadges() {
        return badges.findByUserId(CurrentUser.requireId()).stream()
                .map(b -> new BadgeView(b.getEmoji(), b.getTitle(), b.getCategory()))
                .toList();
    }

    @PatchMapping("/me/settings")
    public ProfileView updateSettings(@RequestBody SettingsUpdate update) {
        Profile p = getOrCreate();
        if (update.language() != null) p.setLanguage(update.language());
        if (update.dailyReminders() != null) p.setDailyReminders(update.dailyReminders());
        return view(profiles.save(p));
    }

    /** Auto-provision a profile on first access; auth-service owns identity. */
    private Profile getOrCreate() {
        String userId = CurrentUser.requireId();
        return profiles.findById(userId).orElseGet(() -> {
            Profile p = new Profile(userId, "Investor", "@investor", "India");
            return profiles.save(p);
        });
    }

    private ProfileView view(Profile p) {
        return new ProfileView(p.getUserId(), p.getName(), p.getHandle(), p.getCity(), p.getLevel(),
                p.getDayStreak(), p.getXpTotal(), p.getLessonsCompleted(), p.getLanguage(),
                p.isDailyReminders(), p.isKycVerified());
    }
}
