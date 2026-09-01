package in.sapphirus.rupee.community.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "posts")
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id")
    private String authorId;

    private String author;
    private String location;
    private String tag;
    private String avatarColor;

    @Column(columnDefinition = "TEXT")
    private String text;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Reply> replies = new ArrayList<>();

    @Column(name = "language", length = 5)
    private String language = "en";

    @Column(name = "upvote_count")
    private int upvoteCount = 0;

    @Column(name = "answer_count")
    private int answerCount = 0;

    @Column(name = "view_count")
    private int viewCount = 0;

    @Column(name = "is_answered")
    private boolean isAnswered = false;

    @Column(name = "accepted_answer_id")
    private UUID acceptedAnswerId;

    @Column(name = "is_removed")
    private boolean isRemoved = false;

    @Column(name = "removed_reason", length = 200)
    private String removedReason;

    @Column(name = "removed_by")
    private UUID removedBy;

    @Column(name = "is_pinned")
    private boolean isPinned = false;

    public Post() {}

    public Post(String authorId, String author, String text, String tag) {
        this.authorId = authorId;
        this.author = author;
        this.text = text;
        this.tag = tag;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getAuthorId() { return authorId; }
    public String getAuthor() { return author; }
    public String getLocation() { return location; }
    public String getTag() { return tag; }
    public String getAvatarColor() { return avatarColor; }
    public String getText() { return text; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<Reply> getReplies() { return replies; }

    public String getLanguage() { return language; }
    public int getUpvoteCount() { return upvoteCount; }
    public int getAnswerCount() { return answerCount; }
    public int getViewCount() { return viewCount; }
    public boolean isAnswered() { return isAnswered; }
    public UUID getAcceptedAnswerId() { return acceptedAnswerId; }
    public boolean isRemoved() { return isRemoved; }
    public String getRemovedReason() { return removedReason; }
    public UUID getRemovedBy() { return removedBy; }
    public boolean isPinned() { return isPinned; }

    public void setAuthorId(String authorId) { this.authorId = authorId; }
    public void setAuthor(String author) { this.author = author; }
    public void setLocation(String location) { this.location = location; }
    public void setTag(String tag) { this.tag = tag; }
    public void setAvatarColor(String avatarColor) { this.avatarColor = avatarColor; }
    public void setText(String text) { this.text = text; }
    public void setLanguage(String language) { this.language = language; }
    public void setUpvoteCount(int upvoteCount) { this.upvoteCount = upvoteCount; }
    public void setAnswerCount(int answerCount) { this.answerCount = answerCount; }
    public void setViewCount(int viewCount) { this.viewCount = viewCount; }
    public void setAnswered(boolean answered) { this.isAnswered = answered; }
    public void setAcceptedAnswerId(UUID acceptedAnswerId) { this.acceptedAnswerId = acceptedAnswerId; }
    public void setRemoved(boolean removed) { this.isRemoved = removed; }
    public void setRemovedReason(String removedReason) { this.removedReason = removedReason; }
    public void setRemovedBy(UUID removedBy) { this.removedBy = removedBy; }
    public void setPinned(boolean pinned) { this.isPinned = pinned; }

    public void addReply(Reply reply) {
        replies.add(reply);
        reply.setPost(this);
        this.answerCount = replies.size();
    }
}

