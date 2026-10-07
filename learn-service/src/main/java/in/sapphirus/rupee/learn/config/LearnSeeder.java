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
            if (jargon.count() < 10) {
                jargon.save(new JargonTerm("Fund Manager",
                        "A SEBI-registered professional who decides which stocks or bonds to buy using your pooled money. They're paid from the expense ratio of the fund.",
                        "Like a master chef who decides what vegetables to buy at the market using everyone's money. You don't cook — they do it for you!",
                        "A Nifty 50 fund manager buys shares in India's top 50 companies. You own a tiny piece of all 50 with just ₹500."));
                jargon.save(new JargonTerm("Mutual Fund",
                        "Group investment + professional management. Returns even when you know nothing about stocks.",
                        "Imagine 1,000 people put ₹500 each. A professional fund manager uses all ₹5 lakh to buy the best ingredients.",
                        "A SIP of ₹500/month into a mutual fund slowly builds wealth without you picking any stocks."));
                jargon.save(new JargonTerm("Money",
                        "A universally accepted medium of exchange for goods and services.",
                        "Token that represents stored energy/work.",
                        "Earning ₹50,000 a month and putting it to work."));
                jargon.save(new JargonTerm("Savings",
                        "Income not spent on immediate consumption, set aside for future use.",
                        "Storing extra grain in a silo after harvest.",
                        "Keeping 20% of your salary in high-yield savings."));
                jargon.save(new JargonTerm("Inflation",
                        "The rate at which general level of prices for goods and services is rising.",
                        "An invisible tax slowly shrinking the size of your samosa over time.",
                        "If inflation is 6%, ₹100 today buys ₹94 worth of goods next year."));
                jargon.save(new JargonTerm("Stock Market",
                        "A public marketplace where buyers and sellers trade shares of publicly held companies.",
                        "A busy vegetable market, but instead of potatoes you buy small pieces of Reliance or TCS.",
                        "NSE and BSE in India."));
                jargon.save(new JargonTerm("Shares",
                        "Units of equity ownership interest in a corporation.",
                        "Owning 1 slice of a 100-slice pizza company.",
                        "Buying 10 shares of Infosys makes you a partial owner."));
                jargon.save(new JargonTerm("NSE",
                        "National Stock Exchange of India, the leading stock exchange located in Mumbai.",
                        "The largest digital stadium where buyers and sellers trade Indian company shares.",
                        "Trading Nifty 50 stocks on NSE."));
                jargon.save(new JargonTerm("Compound Interest",
                        "Interest calculated on the initial principal and also on the accumulated interest of previous periods.",
                        "A snowball rolling down a hill getting bigger and bigger by itself.",
                        "₹10,000 invested at 12% compounding annually turns into ₹31,000 in 10 years."));
                jargon.save(new JargonTerm("NAV",
                        "Net Asset Value: The per-unit market value of all securities held by a mutual fund scheme.",
                        "The price of 1 box of sweet box filled with mixed sweets.",
                        "If a fund holds ₹10 Cr assets and has 10 lakh units, NAV = ₹100."));
                jargon.save(new JargonTerm("Index Fund",
                        "A mutual fund constructed to match or track the components of a financial market index like Nifty 50.",
                        "Copying the smartest student's exam paper line by line without overthinking.",
                        "UTI Nifty 50 Index Fund charging only 0.06% expense ratio."));
                jargon.save(new JargonTerm("SIP",
                        "Systematic Investment Plan: Investing a fixed sum regularly in a mutual fund scheme.",
                        "Putting money into a monthly piggie bank automatically.",
                        "₹1,000 debited automatically on the 5th of every month into an equity fund."));
                jargon.save(new JargonTerm("Lumpsum",
                        "Investing a single large amount of money at one time into a financial product.",
                        "Buying a full year worth of rice upfront instead of monthly packets.",
                        "Investing ₹1,00,000 bonus at once after annual appraisal."));
                jargon.save(new JargonTerm("CAGR",
                        "Compound Annual Growth Rate: The mean annual growth rate of an investment over a specified period longer than one year.",
                        "The smooth average speed of a car trip even if you hit traffic and open highway.",
                        "A portfolio growing from ₹1 Lakh to ₹2 Lakh over 5 years has ~14.87% CAGR."));
                jargon.save(new JargonTerm("ELSS",
                        "Equity Linked Savings Scheme: Tax-saving mutual fund with a 3-year lock-in under Section 80C.",
                        "A dual benefit box: saves income tax today while growing your money in equity.",
                        "Investing ₹1.5 Lakh in ELSS to save up to ₹46,800 in income tax."));
                jargon.save(new JargonTerm("LTCG",
                        "Long Term Capital Gains: Profit made from selling equity held for more than 1 year.",
                        "Tax on rewards earned from patience.",
                        "12.5% tax on equity gains exceeding ₹1.25 Lakh per year."));
                jargon.save(new JargonTerm("Diversification",
                        "Risk management strategy that mixes a wide variety of investments within a portfolio.",
                        "Not putting all your eggs in one basket.",
                        "Spreading ₹10,000 across Large Cap equity, Debt funds, and Gold."));
            }

            // Clean up any legacy les_% rows that disruption chapter ordering
            lessons.findAll().stream()
                    .filter(l -> l.getId() != null && l.getId().startsWith("les_"))
                    .forEach(l -> {
                        try {
                            lessons.deleteById(l.getId());
                        } catch (Exception ignored) {}
                    });

            if (lessons.count() == 0) {
                String segments = "[" +
                        "{\"type\":\"emoji\",\"content\":\"🍲\"}," +
                        "{\"type\":\"text\",\"content\":\"Imagine a dal bhat pot where 1,000 people put in ₹500 each. A professional Fund Manager uses all ₹5 lakh to buy the best ingredients.\"}," +
                        "{\"type\":\"text\",\"content\":\"That's a Mutual Fund!\"}," +
                        "{\"type\":\"callout\",\"title\":\"EASY WAY TO REMEMBER\",\"content\":\"Mutual Fund = Group investment + Professional management.\"}" +
                        "]";
                String jargonWords = "[\"dal bhat pot\",\"Fund Manager\",\"Mutual Fund\"]";
                lessons.save(new Lesson("mf-7", "What is a Mutual Fund?", 2, 1, 6,
                        "Understanding Group Investments", 50, segments, jargonWords));
            }

            if (quiz.count() == 0) {
                quiz.save(new QuizQuestion("q2", "mf-7", "B", "If NAV goes from ₹40 to ₹44, what is your return?",
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
