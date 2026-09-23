package in.sapphirus.rupee.portfolio.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds bse.starmf.* from application.yml.
 * Set STARMF_STUB_MODE=false in production with real credentials.
 */
@ConfigurationProperties(prefix = "bse.starmf")
public class BseStarMfProperties {

    /** BSE StarMF SOAP/REST endpoint. */
    private String baseUrl;

    /** BSE StarMF registered user ID. */
    private String userId;

    /** BSE StarMF password. */
    private String password;

    /** BSE StarMF member code (ARN). */
    private String memberCode;

    /**
     * When true, BseStarMfClient logs orders and returns a fake order ID
     * instead of making real API calls. Safe for development / CI.
     */
    private boolean stubMode = true;

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getMemberCode() { return memberCode; }
    public void setMemberCode(String memberCode) { this.memberCode = memberCode; }

    public boolean isStubMode() { return stubMode; }
    public void setStubMode(boolean stubMode) { this.stubMode = stubMode; }
}
