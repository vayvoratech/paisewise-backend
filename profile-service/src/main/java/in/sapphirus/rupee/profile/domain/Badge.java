package in.sapphirus.rupee.profile.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "badges")
public class Badge {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private java.util.UUID id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    private String emoji;
    private String title;
    private String category;

    protected Badge() {}

    public Badge(String userId, String emoji, String title, String category) {
        this.userId = userId;
        this.emoji = emoji;
        this.title = title;
        this.category = category;
    }

    public java.util.UUID getId() { return id; }
    public String getUserId() { return userId; }
    public String getEmoji() { return emoji; }
    public String getTitle() { return title; }
    public String getCategory() { return category; }
}
