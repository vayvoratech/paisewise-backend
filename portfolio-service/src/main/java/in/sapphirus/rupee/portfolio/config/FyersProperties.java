package in.sapphirus.rupee.portfolio.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "fyers")
public class FyersProperties {

    private String baseUrl = "https://api-t1.fyers.in/api/v3";
    private String appId = "dummy_fyers_app_id";
    private String secretKey = "dummy_fyers_secret_key";
    private String accessToken = "dummy_fyers_access_token";
    private String webhookSecret = "dummy_fyers_webhook_secret";
    private boolean stubMode = true;

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public String getAppId() { return appId; }
    public void setAppId(String appId) { this.appId = appId; }

    public String getSecretKey() { return secretKey; }
    public void setSecretKey(String secretKey) { this.secretKey = secretKey; }

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

    public String getWebhookSecret() { return webhookSecret; }
    public void setWebhookSecret(String webhookSecret) { this.webhookSecret = webhookSecret; }

    public boolean isStubMode() { return stubMode; }
    public void setStubMode(boolean stubMode) { this.stubMode = stubMode; }
}
