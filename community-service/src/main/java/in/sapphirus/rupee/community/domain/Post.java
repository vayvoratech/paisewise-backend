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

    private String authorId;
    private String author;
    private String location;
    private String tag;
    private String avatarColor;

    @Column(length = 2000)
    private String text;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<Reply> replies = new ArrayList<>();

    protected Post() {}

    public Post(String authorId, String author, String location, String tag, String avatarColor, String text) {
        this.authorId = authorId;
        this.author = author;
        this.location = location;
        this.tag = tag;
        this.avatarColor = avatarColor;
        this.text = text;
    }

    public UUID getId() { return id; }
    public String getAuthorId() { return authorId; }
    public String getAuthor() { return author; }
    public String getLocation() { return location; }
    public String getTag() { return tag; }
    public String getAvatarColor() { return avatarColor; }
    public String getText() { return text; }
    public Instant getCreatedAt() { return createdAt; }
    public List<Reply> getReplies() { return replies; }

    public void addReply(Reply r) { r.setPost(this); replies.add(r); }
}
