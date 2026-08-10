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

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(nullable = false, length = 5)
    private String language = "hi";

    @Column(name = "upvote_count", nullable = false)
    private int upvoteCount = 0;

    @Column(name = "answer_count", nullable = false)
    private int answerCount = 0;

    @Column(name = "view_count", nullable = false)
    private int viewCount = 0;

    @Column(name = "is_answered", nullable = false)
    private boolean isAnswered = false;

    @Column(name = "is_removed", nullable = false)
    private boolean isRemoved = false;

    @Column(name = "is_pinned", nullable = false)
    private boolean isPinned = false;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "post_id")
    private List<Reply> replies = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected Post() {}

    public Post(UUID userId, String body, String language) {
        this.userId = userId;
        this.body = body;
        this.language = language;
    }

//    public void addReply(Reply reply) {
//        this.replies.add(reply);
//        this.answerCount = this.replies.size();
//    }

    public void addReply(Reply reply) {
        this.replies.add(reply);
        reply.setPost(this); // <-- This sets the foreign key relationship automatically
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getBody() { return body; }
    public String getLanguage() { return language; }
    public int getUpvoteCount() { return upvoteCount; }
    public int getAnswerCount() { return answerCount; }
    public boolean isAnswered() { return isAnswered; }
    public List<Reply> getReplies() { return replies; }
    public Instant getCreatedAt() { return createdAt; }
}