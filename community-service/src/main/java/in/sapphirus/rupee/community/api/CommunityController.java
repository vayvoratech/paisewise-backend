package in.sapphirus.rupee.community.api;

import in.sapphirus.rupee.community.domain.Post;
import in.sapphirus.rupee.community.domain.Reply;
import in.sapphirus.rupee.community.repo.PostRepository;
import in.sapphirus.rupee.security.CurrentUser;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Beginner-safe Q&A. Routed at /community/**. */
@RestController
@RequestMapping("/community")
public class CommunityController {

    private static final int ONLINE_COUNT = 143;

    private final PostRepository posts;

    public CommunityController(PostRepository posts) {
        this.posts = posts;
    }

    public record ReplyView(String author, boolean verifiedHelper, String text) {}

    public record PostView(String id, String author, String location, String ago, String tag,
                           String avatarColor, String text, List<ReplyView> replies) {}

    public record FeedView(int onlineCount, List<PostView> posts) {}

    public record CreatePostRequest(@NotBlank String text, String tag) {}

    @GetMapping("/feed")
    public FeedView feed() {
        return new FeedView(ONLINE_COUNT, posts.findAllByOrderByCreatedAtDesc().stream().map(this::view).toList());
    }

    @PostMapping("/posts")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public PostView create(@org.springframework.web.bind.annotation.RequestBody CreatePostRequest req) {
        String userId = CurrentUser.requireId();
        Post post = new Post(userId, "You", "India", req.tag() == null ? "#General" : req.tag(), "#6D5DF6", req.text());
        return view(posts.save(post));
    }

    @PostMapping("/posts/{id}/replies")
    @Transactional
    public PostView reply(@PathVariable UUID id,
                          @org.springframework.web.bind.annotation.RequestBody CreatePostRequest req) {
        Post post = posts.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
        post.addReply(new Reply("You", false, req.text()));
        return view(posts.save(post));
    }

    private PostView view(Post p) {
        List<ReplyView> replies = p.getReplies().stream()
                .map(r -> new ReplyView(r.getAuthor(), r.isVerifiedHelper(), r.getText())).toList();
        return new PostView(p.getId().toString(), p.getAuthor(), p.getLocation(), ago(p.getCreatedAt()),
                p.getTag(), p.getAvatarColor(), p.getText(), replies);
    }

    private static String ago(Instant t) {
        long hours = Duration.between(t, Instant.now()).toHours();
        if (hours < 1) return "just now";
        return hours + "h ago";
    }
}
