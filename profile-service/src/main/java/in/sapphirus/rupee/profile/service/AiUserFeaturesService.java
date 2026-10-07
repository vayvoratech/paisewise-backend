package in.sapphirus.rupee.profile.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
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

    private JdbcTemplate learnJdbcTemplate;

    public AiUserFeaturesService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(15000);
        factory.setReadTimeout(15000);
        this.restTemplate = new RestTemplate(factory);
    }

    @PostConstruct
    public void initLearnJdbc() {
        try {
            DriverManagerDataSource ds = new DriverManagerDataSource();
            ds.setDriverClassName("org.postgresql.Driver");
            ds.setUrl("jdbc:postgresql://localhost:5432/learn");
            ds.setUsername("postgres");
            ds.setPassword("Tony");
            this.learnJdbcTemplate = new JdbcTemplate(ds);
            log.info("Connected to secondary Learn DB datasource for live cross-service AI metrics.");
        } catch (Exception e) {
            log.warn("Secondary Learn DB datasource init note: {}", e.getMessage());
        }
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
        result.put("status", "refresh_requested");
        result.put("userId", userId);
        result.put("user_id", userId);
        result.put("feature_version", "v1");
        result.put("updated_at", java.time.Instant.now().toString());
        result.put("features", dynamicFeatures);
        return result;
    }

    private Map<String, Object> calculateRealDynamicFeatures(String userId) {
        Map<String, Object> features = new HashMap<>();

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

        String rawUuidStr = userId.startsWith("usr_") ? userId.substring(4) : userId;

        // 1. Fetch profile stats from profile DB
        if (jdbcTemplate != null) {
            try {
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
            } catch (Exception e) {
                log.warn("Error querying profile DB for {}: {}", userId, e.getMessage());
            }
        }

        // 2. Fetch live quiz attempts & lesson progress directly from learn DB
        if (learnJdbcTemplate != null) {
            try {
                // A. Query learn.quiz_attempts
                List<Map<String, Object>> quizRows = learnJdbcTemplate.queryForList(
                    "SELECT COUNT(*) as q_count, AVG(score_pct) as avg_score, COUNT(CASE WHEN passed THEN 1 END) as passed_cnt FROM learn.quiz_attempts WHERE CAST(user_id AS VARCHAR) LIKE ?", "%" + rawUuidStr + "%"
                );
                if (!quizRows.isEmpty() && quizRows.get(0).get("q_count") != null) {
                    Map<String, Object> qRow = quizRows.get(0);
                    int qCnt = ((Number) qRow.get("q_count")).intValue();
                    if (qCnt > 0) {
                        quizCount = qCnt;
                        if (qRow.get("avg_score") != null) {
                            double rawAvg = ((Number) qRow.get("avg_score")).doubleValue();
                            quizAvgScore = Math.round(rawAvg * 100.0) / 100.0;
                        }
                        int passedCnt = qRow.get("passed_cnt") != null ? ((Number) qRow.get("passed_cnt")).intValue() : 0;
                        lessonPassRate = Math.round(((double) passedCnt / qCnt) * 100.0) / 100.0;
                    }
                }

                // B. Query learn.user_lesson_progress
                List<Map<String, Object>> progressRows = learnJdbcTemplate.queryForList(
                    "SELECT COUNT(*) as p_started, SUM(time_spent_seconds) as total_time FROM learn.user_lesson_progress WHERE CAST(user_id AS VARCHAR) LIKE ?", "%" + rawUuidStr + "%"
                );
                if (!progressRows.isEmpty() && progressRows.get(0).get("p_started") != null) {
                    int pStarted = ((Number) progressRows.get(0).get("p_started")).intValue();
                    if (pStarted > 0 && lessonsStarted == 0) {
                        lessonsStarted = pStarted;
                    }
                    if (progressRows.get(0).get("total_time") != null) {
                        sessionDurationSecs = ((Number) progressRows.get(0).get("total_time")).intValue();
                    }
                }
            } catch (Exception le) {
                log.warn("Error querying learn DB for {}: {}", userId, le.getMessage());
            }
        }

        // 3. Upsert sync to profile.user_features table in profile DB
        if (jdbcTemplate != null) {
            try {
                jdbcTemplate.update(
                    "INSERT INTO profile.user_features (user_id, quiz_attempts_total, avg_quiz_score, quiz_pass_rate, updated_at) " +
                    "VALUES (CAST(? AS UUID), ?, ?, ?, NOW()) " +
                    "ON CONFLICT (user_id) DO UPDATE SET " +
                    "quiz_attempts_total = EXCLUDED.quiz_attempts_total, " +
                    "avg_quiz_score = EXCLUDED.avg_quiz_score, " +
                    "quiz_pass_rate = EXCLUDED.quiz_pass_rate, " +
                    "updated_at = NOW()",
                    rawUuidStr, quizCount, quizAvgScore / 100.0, lessonPassRate
                );
            } catch (Exception ex) {
                log.debug("Sync profile.user_features note: {}", ex.getMessage());
            }
        }

        features.put("age", 25);
        features.put("annual_income", 0.0);
        features.put("monthly_investment", 0.0);
        features.put("portfolio_value", 0.0);
        features.put("risk_profile", (quizCount > 0 || lessonsStarted > 0) ? "Moderate" : "Conservative");
        features.put("investment_experience_years", 0);
        int calculatedMins = sessionDurationSecs > 0 ? (int) Math.ceil((double) sessionDurationSecs / 60.0) : (lessonsStarted * 2);

        features.put("sip_count", 0);
        features.put("kyc_completed", kycVerified);
        features.put("lesson_completion_rate", lessonPassRate);
        features.put("quiz_pass_rate", lessonPassRate);
        features.put("avg_quiz_score", quizAvgScore);
        features.put("quiz_avg_score", quizAvgScore);
        features.put("streak_days", streakDays);
        features.put("total_xp", totalXp);
        features.put("preferred_language", language);
        features.put("paper_trade_count", paperTradesCount);
        features.put("paper_trade_profit_rate", paperWinRate);
        features.put("time_of_day", "morning");
        features.put("session_duration", calculatedMins);
        features.put("screens_visited", screensVisited);
        features.put("lessons_started", lessonsStarted);
        features.put("quiz_attempts_total", quizCount);
        features.put("quizzes_taken", quizCount);

        return features;
    }
}
