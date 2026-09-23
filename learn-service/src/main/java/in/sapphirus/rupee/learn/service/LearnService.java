package in.sapphirus.rupee.learn.service;

import in.sapphirus.rupee.learn.domain.Lesson;
import in.sapphirus.rupee.learn.domain.UserLessonProgress;
import in.sapphirus.rupee.learn.domain.JargonTerm;
import in.sapphirus.rupee.learn.repo.LessonRepository;
import in.sapphirus.rupee.learn.repo.UserLessonProgressRepository;
import in.sapphirus.rupee.learn.repo.JargonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Service layer orchestrating financial education lessons progress and glossary terms lookup.
 */
@Service
public class LearnService {

    private final LessonRepository lessonRepo;
    private final UserLessonProgressRepository progressRepo;
    private final JargonRepository jargonRepo;
    private final StreakService streakService;
    private final XpService xpService;

    public LearnService(LessonRepository lessonRepo,
                        UserLessonProgressRepository progressRepo,
                        JargonRepository jargonRepo,
                        StreakService streakService,
                        XpService xpService) {
        this.lessonRepo = lessonRepo;
        this.progressRepo = progressRepo;
        this.jargonRepo = jargonRepo;
        this.streakService = streakService;
        this.xpService = xpService;
    }

    public List<Lesson> getLessons() {
        return lessonRepo.findAll();
    }

    public Lesson getLesson(String id) {
        return lessonRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found: " + id));
    }

    @Transactional
    public void markLessonViewed(UUID userId, String lessonId) {
        UserLessonProgress progress = progressRepo.findByUserIdAndLessonId(userId, lessonId)
                .orElseGet(() -> new UserLessonProgress(userId, lessonId));

        if ("NOT_STARTED".equals(progress.getStatus())) {
            progress.setStatus("IN_PROGRESS");
        }
        progress.setLastViewedAt(Instant.now());
        progressRepo.save(progress);
        streakService.updateStreak(userId);
    }

    @Transactional
    public void completeLesson(UUID userId, String lessonId) {
        UserLessonProgress progress = progressRepo.findByUserIdAndLessonId(userId, lessonId)
                .orElseGet(() -> new UserLessonProgress(userId, lessonId));

        boolean firstTimeComplete = !"COMPLETED".equalsIgnoreCase(progress.getStatus());
        progress.setStatus("COMPLETED");
        progress.setCompletedAt(Instant.now());
        progress.setLastViewedAt(Instant.now());
        progressRepo.save(progress);

        streakService.updateStreak(userId);
        if (firstTimeComplete) {
            xpService.awardXp(userId, 50, "LESSON_COMPLETE_" + lessonId);
        }
    }

    public double getLessonProgress(UUID userId) {
        long total = lessonRepo.count();
        if (total == 0) {
            return 0.0;
        }
        long completed = progressRepo.findByUserId(userId).stream()
                .filter(p -> "COMPLETED".equalsIgnoreCase(p.getStatus()))
                .count();
        return ((double) completed / total) * 100.0;
    }

    public List<String> getCompletedLessonIds(UUID userId) {
        return progressRepo.findByUserId(userId).stream()
                .filter(p -> "COMPLETED".equalsIgnoreCase(p.getStatus()))
                .map(UserLessonProgress::getLessonId)
                .toList();
    }

    private static final java.util.Map<String, String> aiJargonCache = new java.util.concurrent.ConcurrentHashMap<>();

    public JargonTerm getJargonTerm(String term) {
        if (term == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Term is null");
        }
        String cleanTerm = term.trim().replaceAll("-", " ");
        return jargonRepo.findByTermIgnoreCase(term)
                .or(() -> jargonRepo.findByTermIgnoreCase(cleanTerm))
                .or(() -> jargonRepo.findFirstByTermContainingIgnoreCase(term))
                .or(() -> jargonRepo.findFirstByTermContainingIgnoreCase(cleanTerm))
                .or(() -> jargonRepo.findById(term))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Jargon term not found: " + term));
    }

    private String expandTerm(String term) {
        if (term == null) return "";
        String t = term.trim();
        if ("fd".equalsIgnoreCase(t)) return "Fixed Deposit";
        if ("rd".equalsIgnoreCase(t)) return "Recurring Deposit";
        if ("mf".equalsIgnoreCase(t)) return "Mutual Fund";
        if ("pe".equalsIgnoreCase(t) || "p/e".equalsIgnoreCase(t)) return "Price to Earnings Ratio";
        if ("nav".equalsIgnoreCase(t)) return "Net Asset Value";
        if ("sip".equalsIgnoreCase(t)) return "Systematic Investment Plan";
        if ("nifty".equalsIgnoreCase(t)) return "Nifty 50";
        if ("sensex".equalsIgnoreCase(t)) return "BSE Sensex";
        return t;
    }

    public String getAiJargonExplanation(String term, String language) {
        if (term == null || term.isBlank()) {
            return "Please select a valid financial term.";
        }
        String queryTerm = expandTerm(term);
        String langCode = normalizeLanguageCode(language);
        String cacheKey = term.trim().toLowerCase() + ":" + langCode;

        if (aiJargonCache.containsKey(cacheKey)) {
            return aiJargonCache.get(cacheKey);
        }

        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                    .connectTimeout(java.time.Duration.ofSeconds(15))
                    .build();

            String jsonPayload = String.format("{\"term\":\"%s\",\"language\":\"%s\",\"userId\":\"1\"}",
                    queryTerm.replaceAll("\"", "\\\""), langCode);

            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("https://paisewise-aiml-dgd5.onrender.com/ai/jargon"))
                    .header("Content-Type", "application/json")
                    .timeout(java.time.Duration.ofSeconds(15))
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 && response.body() != null && response.body().contains("explanation")) {
                com.fasterxml.jackson.databind.JsonNode root = new com.fasterxml.jackson.databind.ObjectMapper().readTree(response.body());
                if (root.has("explanation")) {
                    String explanation = root.get("explanation").asText();
                    if (explanation != null && !explanation.isBlank()) {
                        aiJargonCache.put(cacheKey, explanation);
                        return explanation;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("AI Jargon API call timed out or failed (>15s): " + e.getMessage());
        }

        // Check local DB term
        try {
            JargonTerm local = getJargonTerm(term);
            return String.format("### Plain Explanation\n\n%s\n\n### Everyday Analogy\n\n%s\n\n### INR Example\n\n%s",
                    local.getDefinition(), local.getAnalogy(), local.getExample());
        } catch (Exception ex) {
            // Fallback rich definitions for common terms if external AI fails or returns unavailable
            if ("fd".equalsIgnoreCase(term) || "fixed deposit".equalsIgnoreCase(queryTerm)) {
                return "### Plain Explanation\n\nA Fixed Deposit (FD) is a secure financial investment offered by banks where you deposit a lump sum for a fixed period at a guaranteed interest rate.\n\n### Everyday Analogy\n\nLike locking money in a bank vault where the bank pays you extra interest for keeping it untouched.\n\n### INR Example\n\nInvesting ₹1,00,000 in a 1-year FD at 7% interest yields ₹1,07,000 upon maturity.";
            }
            if ("purchasing power".equalsIgnoreCase(term) || "purchasing power".equalsIgnoreCase(queryTerm)) {
                return "### Plain Explanation\n\nPurchasing Power is the financial value of money expressed in terms of the amount of goods or services that one unit of money can buy.\n\n### Everyday Analogy\n\nIf ₹100 bought 5 samosas last year, but only 4 samosas today, your money's purchasing power has dropped.\n\n### INR Example\n\nDue to 6% annual inflation, ₹1,00,000 saved under a mattress has a purchasing power of only ₹94,339 after one year.";
            }
            if ("inflation".equalsIgnoreCase(term) || "inflation".equalsIgnoreCase(queryTerm)) {
                return "### Plain Explanation\n\nInflation is the rate at which the general level of prices for goods and services rises, causing purchasing power to fall over time.\n\n### Everyday Analogy\n\nAn invisible tax that slowly shrinks the purchasing capacity of cash stored idle without earning returns.\n\n### INR Example\n\nA movie ticket that cost ₹150 three years ago now costs ₹220 due to inflation.";
            }
            return "### Plain Explanation\n\n" + queryTerm + " is a core financial concept in trading and investing.\n\n### Everyday Analogy\n\nBuilding disciplined financial knowledge helps turn market volatility into long-term compounding benefits.\n\n### INR Example\n\nConsistent monthly investing builds wealth over a 5 to 10 year horizon.";
        }
    }

    private String getAnalogy(String term) {
        if ("fixed deposit".equalsIgnoreCase(term) || "fd".equalsIgnoreCase(term)) return "Like locking money in a bank vault where the bank pays you extra interest for keeping it untouched.";
        if ("inflation".equalsIgnoreCase(term)) return "An invisible tax that slowly shrinks the purchasing capacity of cash stored idle without earning returns.";
        if ("purchasing power".equalsIgnoreCase(term)) return "If ₹100 bought 5 samosas last year, but only 4 samosas today, your money's purchasing power has dropped.";
        return "Building disciplined financial knowledge helps turn market volatility into long-term compounding benefits.";
    }

    private String getExample(String term) {
        if ("fixed deposit".equalsIgnoreCase(term) || "fd".equalsIgnoreCase(term)) return "Investing ₹1,00,000 in a 1-year FD at 7% interest yields ₹1,07,000 upon maturity.";
        if ("inflation".equalsIgnoreCase(term)) return "A movie ticket that cost ₹150 three years ago now costs ₹220 due to inflation.";
        if ("purchasing power".equalsIgnoreCase(term)) return "Due to 6% annual inflation, ₹1,00,000 saved under a mattress has a purchasing power of only ₹94,339 after one year.";
        return "Consistent monthly investing builds wealth over a 5 to 10 year horizon.";
    }

    private String normalizeLanguageCode(String lang) {
        if (lang == null) return "en";
        String l = lang.trim().toLowerCase();
        if (l.contains("telugu") || "te".equals(l)) return "te";
        if (l.contains("hindi") || "hi".equals(l)) return "hi";
        if (l.contains("bengali") || "bn".equals(l)) return "bn";
        if (l.contains("gujarati") || "gu".equals(l)) return "gu";
        if (l.contains("tamil") || "ta".equals(l)) return "ta";
        if (l.contains("kannada") || "kn".equals(l)) return "kn";
        if (l.contains("marathi") || "mr".equals(l)) return "mr";
        return "en";
    }
}
