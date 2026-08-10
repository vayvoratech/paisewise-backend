package in.sapphirus.rupee.community.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "replies")
public class Reply {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    @JsonIgnore
    private Post post;

    @Column(name = "user_id")
    private String author; // author ID

    @Column(name = "is_verified_helper")
    private boolean verifiedHelper;

    @Column(columnDefinition = "TEXT")
    private String text;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    // Advanced Columns from community_answers DDL
    @Column(name = "upvote_count")
    private int upvoteCount = 0;

    @Column(name = "is_accepted")
    private boolean isAccepted = false;

    @Column(name = "is_removed")
    private boolean isRemoved = false;

    @Column(name = "removed_reason", length = 200)
    private String removedReason;

    protected Reply() {}

    public Reply(String author, boolean verifiedHelper, String text) {
        this.author = author;
        this.verifiedHelper = verifiedHelper;
        this.text = text;
    }

    public UUID getId() { return id; }
    public Post getPost() { return post; }
    public void setPost(Post post) { this.post = post; }
    public String getAuthor() { return author; }
    public boolean isVerifiedHelper() { return verifiedHelper; }
    public String getText() { return text; }
    public Instant getCreatedAt() { return createdAt; }

    public int getUpvoteCount() { return upvoteCount; }
    public boolean isAccepted() { return isAccepted; }
    public boolean isRemoved() { return isRemoved; }
    public String getRemovedReason() { return removedReason; }

    public void setUpvoteCount(int upvoteCount) { this.upvoteCount = upvoteCount; }
    public void setAccepted(boolean accepted) { this.isAccepted = accepted; }
    public void setRemoved(boolean removed) { this.isRemoved = removed; }
    public void setRemovedReason(String removedReason) { this.removedReason = removedReason; }
}
