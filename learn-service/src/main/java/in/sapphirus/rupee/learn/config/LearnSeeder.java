package in.sapphirus.rupee.learn.config;

import in.sapphirus.rupee.learn.domain.JargonTerm;
import in.sapphirus.rupee.learn.domain.Lesson;
import in.sapphirus.rupee.learn.domain.QuizQuestion;
import in.sapphirus.rupee.learn.repo.JargonRepository;
import in.sapphirus.rupee.learn.repo.LessonRepository;
import in.sapphirus.rupee.learn.repo.QuizRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Seeds learning content on first startup. Mirrors the mobile app's learn.data.ts. */
@Configuration
public class LearnSeeder {

    @Bean
    CommandLineRunner seedLearn(LessonRepository lessons, JargonRepository jargon, QuizRepository quiz) {
        return args -> {
            if (jargon.count() == 0) {
                jargon.save(new JargonTerm("Fund Manager",
                        "A SEBI-registered professional who decides which stocks or bonds to buy using your pooled money. They're paid from the expense ratio of the fund.",
                        "Like a master chef who decides what vegetables to buy at the market using everyone's money. You don't cook — they do it for you!",
                        "A Nifty 50 fund manager buys shares in India's top 50 companies. You own a tiny piece of all 50 with just ₹500."));
                jargon.save(new JargonTerm("Mutual Fund",
                        "Group investment + professional management. Returns even when you know nothing about stocks.",
                        "Imagine 1,000 people put ₹500 each. A professional fund manager uses all ₹5 lakh to buy the best ingredients.",
                        "A SIP of ₹500/month into a mutual fund slowly builds wealth without you picking any stocks."));
            }

            if (lessons.count() == 0) {
                String segments = "[" +
                        "{\"type\":\"emoji\",\"content\":\"🍲\"}," +
                        "{\"type\":\"text\",\"content\":\"Imagine a dal bhat pot where 1,000 people put in ₹500 each. A professional Fund Manager uses all ₹5 lakh to buy the best ingredients.\"}," +
                        "{\"type\":\"text\",\"content\":\"That's a Mutual Fund!\"}," +
                        "{\"type\":\"callout\",\"title\":\"EASY WAY TO REMEMBER\",\"content\":\"Mutual Fund = Group investment + Professional management.\"}" +
                        "]";
                String jargonWords = "[\"dal bhat pot\",\"Fund Manager\",\"Mutual Fund\"]";
                lessons.save(new Lesson("mf-3", "Mutual Funds", 3, 3, 5,
                        "What exactly is a Mutual Fund?", 50, segments, jargonWords));
            }

            if (quiz.count() == 0) {
                quiz.save(new QuizQuestion("q2", "If NAV goes from ₹40 to ₹44, what is your return?",
                        25, 50, 1,
                        "[{\"key\":\"A\",\"text\":\"4% return on investment\",\"correct\":false}," +
                        "{\"key\":\"B\",\"text\":\"10% return on investment\",\"correct\":true}," +
                        "{\"key\":\"C\",\"text\":\"Just ₹4 profit, no %\",\"correct\":false}," +
                        "{\"key\":\"D\",\"text\":\"Can't calculate without more info\",\"correct\":false}]",
                        "Correct! (44−40)÷40 × 100 = 10%."));
            }
        };
    }
}
