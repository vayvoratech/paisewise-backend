package in.sapphirus.rupee.auth.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 20)
    private String channel;

    @Column(nullable = false, length = 50)
    private String type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(columnDefinition = "TEXT") // Mapped as String for JSONB fallback
    private String data;

    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "fcm_message_id", length = 100)
    private String fcmMessageId;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "deep_link", length = 500)
    private String deepLink;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Notification() {}

    public Notification(UUID userId, String channel, String type, String title, String body) {
        this.userId = userId;
        this.channel = channel;
        this.type = type;
        this.title = title;
        this.body = body;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getChannel() { return channel; }
    public String getType() { return type; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public String getData() { return data; }
    public String getStatus() { return status; }
    public String getFcmMessageId() { return fcmMessageId; }
    public String getErrorMessage() { return errorMessage; }
    public boolean isRead() { return isRead; }
    public Instant getReadAt() { return readAt; }
    public String getDeepLink() { return deepLink; }
    public String getImageUrl() { return imageUrl; }
    public Instant getSentAt() { return sentAt; }
    public Instant getCreatedAt() { return createdAt; }

    public void setData(String data) { this.data = data; }
    public void setStatus(String status) { this.status = status; }
    public void setFcmMessageId(String fcmMessageId) { this.fcmMessageId = fcmMessageId; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public void setRead(boolean read) { isRead = read; }
    public void setReadAt(Instant readAt) { this.readAt = readAt; }
    public void setDeepLink(String deepLink) { this.deepLink = deepLink; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setSentAt(Instant sentAt) { this.sentAt = sentAt; }
}
