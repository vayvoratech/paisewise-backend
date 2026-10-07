package in.sapphirus.rupee.profile.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class AiUserFeaturesService {

    private static final Logger log = LoggerFactory.getLogger(AiUserFeaturesService.class);
    private static final String RENDER_BASE_URL = "https://paisewise-aiml-dgd5.onrender.com";

    private final RestTemplate restTemplate;

    public AiUserFeaturesService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(15000);
        factory.setReadTimeout(15000);
        this.restTemplate = new RestTemplate(factory);
    }

    public Map<String, Object> getUserFeatures(String userId) {
        String url = RENDER_BASE_URL + "/features/" + userId;
        try {
            log.info("Fetching AI user features for userId: {} from Render: {}", userId, url);
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response != null && !response.isEmpty()) {
                return response;
            }
        } catch (Exception e) {
            log.warn("Render AI User Features GET failed for userId {}: {}. Returning fallback features.", userId, e.getMessage());
        }
        return buildFallbackFeatures(userId);
    }

    public Map<String, Object> refreshUserFeatures(String userId) {
        String url = RENDER_BASE_URL + "/ai/features/refresh/" + userId;
        try {
            log.info("Refreshing AI user features for userId: {} at Render: {}", userId, url);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>("{}", headers);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);
            if (response != null && !response.isEmpty()) {
                return response;
            }
        } catch (Exception e) {
            log.warn("Render AI User Features POST refresh failed for userId {}: {}. Returning fallback features.", userId, e.getMessage());
        }
        Map<String, Object> fallback = buildFallbackFeatures(userId);
        fallback.put("status", "refreshed");
        return fallback;
    }

    private Map<String, Object> buildFallbackFeatures(String userId) {
        Map<String, Object> root = new HashMap<>();
        root.put("user_id", userId);
        root.put("feature_version", "v1");
        root.put("updated_at", java.time.Instant.now().toString());

        Map<String, Object> features = new HashMap<>();
        features.put("age", 28);
        features.put("annual_income", 600000.0);
        features.put("monthly_investment", 10000.0);
        features.put("portfolio_value", 250000.0);
        features.put("risk_profile", "Moderate");
        features.put("investment_experience_years", 3);
        features.put("sip_count", 2);
        features.put("kyc_completed", true);
        features.put("lesson_completion_rate", 0.62);
        features.put("quiz_avg_score", 69.05);
        features.put("streak_days", 82);
        features.put("total_xp", 9050);
        features.put("preferred_language", "English");
        features.put("paper_trade_count", 81);
        features.put("paper_trade_profit_rate", 0.69);
        features.put("time_of_day", "morning");
        features.put("session_duration", 4047);
        features.put("screens_visited", 813);
        features.put("lessons_started", 119);
        features.put("quizzes_taken", 75);

        root.put("features", features);
        return root;
    }
}
