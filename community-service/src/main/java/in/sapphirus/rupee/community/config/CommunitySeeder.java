package in.sapphirus.rupee.community.config;

import in.sapphirus.rupee.community.domain.Post;
import in.sapphirus.rupee.community.domain.Reply;
import in.sapphirus.rupee.community.repo.PostRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Seeds sample community posts (mirrors the mobile app's community.data.ts). */
@Configuration
public class CommunitySeeder {

    @Bean
    CommandLineRunner seedCommunity(PostRepository posts) {
        return args -> {
            if (posts.count() > 0) return;

            Post p1 = new Post("seed", "Priya", "Jaipur, Rajasthan", "#MutualFunds", "#EF4444",
                    "SIP mein ₹500 se start kar sakti hoon? Kya itna kam kaafi hai? 😅");
            p1.addReply(new Reply("Amit K.", true,
                    "Haan bilkul! ₹100 se bhi SIP hoti hai. Small se shuru karo, habit banao! 🙌"));
            posts.save(p1);

            Post p2 = new Post("seed", "Rajan", "Indore, MP", "#Stocks", "#3B82F6",
                    "Market crash ho jaye toh mera paisa dub jayega kya? Bahut darr lagta hai 😨");
            p2.addReply(new Reply("Kavya M.", true,
                    "Long term mein Sensex hamesha upar gaya hai! Practice mode mein try karo pehle 💪"));
            posts.save(p2);
        };
    }
}
