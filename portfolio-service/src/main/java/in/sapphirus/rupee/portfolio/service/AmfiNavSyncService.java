package in.sapphirus.rupee.portfolio.service;

import in.sapphirus.rupee.portfolio.domain.MfScheme;
import in.sapphirus.rupee.portfolio.repo.MfSchemeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service and Scheduled Job for synchronizing daily mutual fund Net Asset Values (NAV)
 * directly from the official AMFI (Association of Mutual Funds in India) portal.
 *
 * <p>Scheduled to run daily at 7:00 PM IST (19:00 IST) when AMCs publish the day's NAVs.</p>
 */
@Service
public class AmfiNavSyncService {

    private static final Logger log = LoggerFactory.getLogger(AmfiNavSyncService.class);
    private static final String DEFAULT_AMFI_NAV_URL = "https://www.amfiindia.com/spages/NAVAll.txt";
    private static final DateTimeFormatter AMFI_DATE_FORMAT = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("dd-MMM-yyyy")
            .toFormatter(Locale.ENGLISH);

    private final MfSchemeRepository schemeRepo;
    private final RestClient restClient;
    private final String amfiUrl;

    public AmfiNavSyncService(MfSchemeRepository schemeRepo,
                               @Value("${amfi.nav.url:" + DEFAULT_AMFI_NAV_URL + "}") String amfiUrl) {
        this.schemeRepo = schemeRepo;
        this.amfiUrl = amfiUrl;
        this.restClient = RestClient.builder().build();
    }

    /**
     * Daily AMFI NAV Sync Job.
     * Runs every day at 7:00 PM (19:00:00) Asia/Kolkata timezone.
     */
    @Scheduled(cron = "${amfi.nav.sync.cron:0 0 19 * * *}", zone = "Asia/Kolkata")
    public void runDailyNavSync() {
        log.info("Starting scheduled AMFI NAV sync job at 7:00 PM IST...");
        SyncResult result = performNavSync();
        log.info("Scheduled AMFI NAV sync completed: processed={}, updated={}, newInserted={}, errors={}, elapsedMs={}",
                result.totalParsed(), result.schemesUpdated(), result.schemesInserted(), result.errors(), result.elapsedTimeMs());
    }

    /**
     * Manually trigger AMFI NAV sync (for admin or testing).
     */
    public SyncResult syncNavNow() {
        log.info("Manual AMFI NAV sync triggered");
        return performNavSync();
    }

    /**
     * Core sync execution method.
     */
    @Transactional
    public SyncResult performNavSync() {
        long startTime = System.currentTimeMillis();
        int totalParsed = 0;
        int updated = 0;
        int inserted = 0;
        int errors = 0;

        try {
            log.info("Fetching AMFI NAV file from: {}", amfiUrl);
            String rawContent = restClient.get()
                    .uri(amfiUrl)
                    .retrieve()
                    .body(String.class);

            if (rawContent == null || rawContent.isBlank()) {
                log.warn("AMFI NAV response was empty");
                return new SyncResult(0, 0, 0, 1, System.currentTimeMillis() - startTime, "Empty response from AMFI");
            }

            Map<String, MfScheme> existingSchemes = new HashMap<>();
            for (MfScheme s : schemeRepo.findAll()) {
                existingSchemes.put(s.getSchemeCode(), s);
            }

            List<MfScheme> toSave = new ArrayList<>();
            String currentAmc = "Unknown AMC";
            String currentCategory = "Other";

            String[] lines = rawContent.split("\\r?\\n");
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                // Check for AMC header (e.g., "Aditya Birla Sun Life Mutual Fund")
                if (line.endsWith("Mutual Fund") || line.contains("Mutual Fund (") || line.endsWith("Trust")) {
                    currentAmc = cleanAmcName(line);
                    continue;
                }

                // Check for Category header (e.g., "Open Ended Schemes ( Equity Scheme - Large Cap Fund )")
                if (line.startsWith("Open Ended Schemes") || line.startsWith("Close Ended Schemes") || line.startsWith("Interval Fund Schemes")) {
                    currentCategory = extractCategory(line);
                    continue;
                }

                // Header line check: Scheme Code;ISIN Div Payout/ ISIN Growth;ISIN Div Reinvestment;Scheme Name;Net Asset Value;Date
                if (line.startsWith("Scheme Code") || line.startsWith("Scheme_Code")) {
                    continue;
                }

                String[] tokens = line.split(";");
                if (tokens.length < 6) {
                    continue;
                }

                String schemeCode = tokens[0].trim();
                String isin = tokens[1].trim();
                String schemeName = tokens[3].trim();
                String navStr = tokens[4].trim();
                String dateStr = tokens[5].trim();

                if (schemeCode.isEmpty() || navStr.equalsIgnoreCase("N.A.") || navStr.isEmpty()) {
                    continue;
                }

                try {
                    double nav = Double.parseDouble(navStr);
                    LocalDate navDate = parseDate(dateStr);
                    totalParsed++;

                    if (existingSchemes.containsKey(schemeCode)) {
                        MfScheme existing = existingSchemes.get(schemeCode);
                        existing.setNav(nav);
                        if (navDate != null) {
                            existing.setNavDate(navDate);
                        }
                        if (existing.getIsin() == null || existing.getIsin().isBlank()) {
                            existing.setIsin(isin);
                        }
                        existing.setUpdatedAt(Instant.now());
                        toSave.add(existing);
                        updated++;
                    }
                } catch (Exception e) {
                    errors++;
                }

                if (toSave.size() >= 500) {
                    schemeRepo.saveAll(toSave);
                    toSave.clear();
                }
            }

            if (!toSave.isEmpty()) {
                schemeRepo.saveAll(toSave);
            }

            long elapsed = System.currentTimeMillis() - startTime;
            return new SyncResult(totalParsed, updated, inserted, errors, elapsed, "SUCCESS");

        } catch (Exception e) {
            log.error("Failed to execute AMFI NAV sync: {}", e.getMessage(), e);
            long elapsed = System.currentTimeMillis() - startTime;
            return new SyncResult(totalParsed, updated, inserted, errors + 1, elapsed, "FAILED: " + e.getMessage());
        }
    }

    private String cleanAmcName(String amcLine) {
        String clean = amcLine.trim();
        int idx = clean.indexOf("Mutual Fund");
        if (idx > 0) {
            return clean.substring(0, idx).trim();
        }
        return clean;
    }

    private String extractCategory(String catLine) {
        Pattern pattern = Pattern.compile("\\((.*?)\\)");
        Matcher matcher = pattern.matcher(catLine);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "Equity Scheme";
    }

    private LocalDate parseDate(String dateStr) {
        try {
            return LocalDate.parse(dateStr, AMFI_DATE_FORMAT);
        } catch (Exception e) {
            return LocalDate.now();
        }
    }

    public record SyncResult(
            int totalParsed,
            int schemesUpdated,
            int schemesInserted,
            int errors,
            long elapsedTimeMs,
            String status
    ) {}
}
