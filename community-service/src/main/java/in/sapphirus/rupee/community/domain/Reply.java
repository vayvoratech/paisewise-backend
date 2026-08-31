package in.sapphirus.rupee.community.domain;

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
    @JoinColumn(name = "post_id")
    private Post post;

    private String author;
    private boolean verifiedHelper;

    @Column(length = 2000)
    private String text;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

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
}
