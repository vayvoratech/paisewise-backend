package in.sapphirus.rupee.profile.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "badge_definitions", schema = "public")
public class BadgeDefinition {
    @Id
    @Column(name = "badge_key", length = 50)
    private String badgeKey;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(name = "name_en", nullable = false, length = 100)
    private String nameEn;

    @Column(name = "name_hi", nullable = false, length = 100)
    private String nameHi;

    @Column(name = "description_en", nullable = false, columnDefinition = "TEXT")
    private String descriptionEn;

    @Column(name = "description_hi", nullable = false, columnDefinition = "TEXT")
    private String descriptionHi;

    @Column(name = "icon_emoji", nullable = false, length = 10)
    private String iconEmoji;

    @Column(name = "xp_bonus", nullable = false)
    private Integer xpBonus = 0;

    @Column(name = "unlock_criteria_en", nullable = false, columnDefinition = "TEXT")
    private String unlockCriteriaEn;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public BadgeDefinition() {}

    public BadgeDefinition(String badgeKey, String category, String nameEn, String nameHi, String descriptionEn, String descriptionHi, String iconEmoji, Integer xpBonus, String unlockCriteriaEn, Integer sortOrder) {
        this.badgeKey = badgeKey;
        this.category = category;
        this.nameEn = nameEn;
        this.nameHi = nameHi;
        this.descriptionEn = descriptionEn;
        this.descriptionHi = descriptionHi;
        this.iconEmoji = iconEmoji;
        this.xpBonus = xpBonus;
        this.unlockCriteriaEn = unlockCriteriaEn;
        this.sortOrder = sortOrder;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    // Getters and Setters
    public String getBadgeKey() { return badgeKey; }
    public void setBadgeKey(String badgeKey) { this.badgeKey = badgeKey; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getNameEn() { return nameEn; }
    public void setNameEn(String nameEn) { this.nameEn = nameEn; }
    public String getNameHi() { return nameHi; }
    public void setNameHi(String nameHi) { this.nameHi = nameHi; }
    public String getDescriptionEn() { return descriptionEn; }
    public void setDescriptionEn(String descriptionEn) { this.descriptionEn = descriptionEn; }
    public String getDescriptionHi() { return descriptionHi; }
    public void setDescriptionHi(String descriptionHi) { this.descriptionHi = descriptionHi; }
    public String getIconEmoji() { return iconEmoji; }
    public void setIconEmoji(String iconEmoji) { this.iconEmoji = iconEmoji; }
    public Integer getXpBonus() { return xpBonus; }
    public void setXpBonus(Integer xpBonus) { this.xpBonus = xpBonus; }
    public String getUnlockCriteriaEn() { return unlockCriteriaEn; }
    public void setUnlockCriteriaEn(String unlockCriteriaEn) { this.unlockCriteriaEn = unlockCriteriaEn; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
