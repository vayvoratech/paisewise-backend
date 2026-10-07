package in.sapphirus.rupee.profile.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiUserFeaturesService {

    private static final Logger log = LoggerFactory.getLogger(AiUserFeaturesService.class);
    private static final String RENDER_BASE_URL = "https://paisewise-aiml-dgd5.onrender.com";

    private final RestTemplate restTemplate;

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    public AiUserFeaturesService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(15000);
        factory.setReadTimeout(15000);
        this.restTemplate = new RestTemplate(factory);
    }

    public Map<String, Object> getUserFeatures(String userId) {
        Map<String, Object> result = new HashMap<>();
        String url = RENDER_BASE_URL + "/features/" + userId;
        try {
            log.info("Fetching AI user features for userId: {} from Render: {}", userId, url);
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response != null && !response.isEmpty()) {
                result.putAll(response);
            }
        } catch (Exception e) {
            log.warn("Render AI User Features GET failed for userId {}: {}. Falling back to dynamic DB calculation.", userId, e.getMessage());
        }

        // Overlay real dynamic database features for the user
        Map<String, Object> dynamicFeatures = calculateRealDynamicFeatures(userId);

        if (result.containsKey("features") && result.get("features") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> existingFeatures = (Map<String, Object>) result.get("features");
            existingFeatures.putAll(dynamicFeatures);
        } else {
            result.put("user_id", userId);
            result.put("feature_version", "v1");
            result.put("updated_at", java.time.Instant.now().toString());
            result.put("features", dynamicFeatures);
        }

        return result;
    }

    public Map<String, Object> refreshUserFeatures(String userId) {
        Map<String, Object> result = new HashMap<>();
        String url = RENDER_BASE_URL + "/ai/features/refresh/" + userId;
        try {
            log.info("Refreshing AI user features for userId: {} at Render: {}", userId, url);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>("{}", headers);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);
            if (response != null && !response.isEmpty()) {
                result.putAll(response);
            }
        } catch (Exception e) {
            log.warn("Render AI User Features POST refresh failed for userId {}: {}. Falling back to dynamic DB calculation.", userId, e.getMessage());
        }

        Map<String, Object> dynamicFeatures = calculateRealDynamicFeatures(userId);
        if (result.containsKey("features") && result.get("features") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> existingFeatures = (Map<String, Object>) result.get("features");
            existingFeatures.putAll(dynamicFeatures);
        } else {
            result.put("user_id", userId);
            result.put("feature_version", "v1");
            result.put("updated_at", java.time.Instant.now().toString());
            result.put("features", dynamicFeatures);
        }
        result.put("status", "refreshed");
        result.put("updated_at", java.time.Instant.now().toString());
        return result;
    }

    private Map<String, Object> calculateRealDynamicFeatures(String userId) {
        Map<String, Object> features = new HashMap<>();

        // Defaults for new / empty user records (0 / zeroed out until user acts)
        double quizAvgScore = 0.0;
        int quizCount = 0;
        double lessonPassRate = 0.0;
        int streakDays = 0;
        int totalXp = 0;
        int lessonsStarted = 0;
        String language = "English";
        boolean kycVerified = false;
        int paperTradesCount = 0;
        double paperWinRate = 0.0;
        int sessionDurationSecs = 0;
        int screensVisited = 0;

        if (jdbcTemplate != null) {
            try {
                String rawUuidStr = userId.startsWith("usr_") ? userId.substring(4) : userId;

                // 1. Fetch real streak, XP, language, lessons from profile.profiles
                List<Map<String, Object>> profileRows = jdbcTemplate.queryForList(
                    "SELECT xp_total, day_streak, lessons_completed, language, kyc_verified FROM profile.profiles WHERE CAST(user_id AS VARCHAR) LIKE ?", "%" + rawUuidStr + "%"
                );
                if (!profileRows.isEmpty()) {
                    Map<String, Object> pRow = profileRows.get(0);
                    if (pRow.get("xp_total") != null) totalXp = ((Number) pRow.get("xp_total")).intValue();
                    if (pRow.get("day_streak") != null) streakDays = ((Number) pRow.get("day_streak")).intValue();
                    if (pRow.get("lessons_completed") != null) lessonsStarted = ((Number) pRow.get("lessons_completed")).intValue();
                    if (pRow.get("language") != null) language = (String) pRow.get("language");
                    if (pRow.get("kyc_verified") != null) kycVerified = (Boolean) pRow.get("kyc_verified");
                }

                // 2. Fetch real avg quiz score & quiz count & paper trade stats from profile.user_features
                try {
                    List<Map<String, Object>> ufRows = jdbcTemplate.queryForList(
                        "SELECT avg_quiz_score, quiz_pass_rate, quiz_attempts_total, paper_trades_total, avg_session_duration_secs FROM profile.user_features WHERE CAST(user_id AS VARCHAR) LIKE ?", "%" + rawUuidStr + "%"
                    );
                    if (!ufRows.isEmpty()) {
                        Map<String, Object> uf = ufRows.get(0);
                        if (uf.get("avg_quiz_score") != null) {
                            double val = ((Number) uf.get("avg_quiz_score")).doubleValue();
                            quizAvgScore = (val <= 1.0 && val > 0) ? Math.round(val * 10000.0) / 100.0 : Math.round(val * 100.0) / 100.0;
                        }
                        if (uf.get("quiz_pass_rate") != null) {
                            double val = ((Number) uf.get("quiz_pass_rate")).doubleValue();
                            lessonPassRate = (val <= 1.0 && val > 0) ? Math.round(val * 10000.0) / 10000.0 : Math.round(val * 100.0) / 100.0;
                        }
                        if (uf.get("quiz_attempts_total") != null) quizCount = ((Number) uf.get("quiz_attempts_total")).intValue();
                        if (uf.get("paper_trades_total") != null) paperTradesCount = ((Number) uf.get("paper_trades_total")).intValue();
                        if (uf.get("avg_session_duration_secs") != null) sessionDurationSecs = ((Number) uf.get("avg_session_duration_secs")).intValue();
                    } else {
                        // Insert an initial zeroed row in profile.user_features so database triggers work when user acts
                        try {
                            jdbcTemplate.update(
                                "INSERT INTO profile.user_features (user_id, updated_at) VALUES (CAST(? AS UUID), NOW()) ON CONFLICT (user_id) DO NOTHING",
                                rawUuidStr
                            );
                        } catch (Exception ex) {
                            log.debug("Auto-insert user_features note: {}", ex.getMessage());
                        }
                    }
                } catch (Exception ufe) {
                    log.debug("Profile DB user_features query note: {}", ufe.getMessage());
                }
            } catch (Exception e) {
                log.warn("Error calculating dynamic DB user features for {}: {}", userId, e.getMessage());
            }
        }

        features.put("age", 25);
        features.put("annual_income", 0.0);
        features.put("monthly_investment", 0.0);
        features.put("portfolio_value", 0.0);
        features.put("risk_profile", quizCount > 0 ? "Moderate" : "Conservative");
        features.put("investment_experience_years", 0);
        features.put("sip_count", 0);
        features.put("kyc_completed", kycVerified);
        features.put("lesson_completion_rate", lessonPassRate);
        features.put("quiz_avg_score", quizAvgScore);
        features.put("streak_days", streakDays);
        features.put("total_xp", totalXp);
        features.put("preferred_language", language);
        features.put("paper_trade_count", paperTradesCount);
        features.put("paper_trade_profit_rate", paperWinRate);
        features.put("time_of_day", "morning");
        features.put("session_duration", sessionDurationSecs > 0 ? Math.round((float) sessionDurationSecs / 60.0) : 0);
        features.put("screens_visited", screensVisited);
        features.put("lessons_started", lessonsStarted);
        features.put("quizzes_taken", quizCount);

        return features;
    }
}
