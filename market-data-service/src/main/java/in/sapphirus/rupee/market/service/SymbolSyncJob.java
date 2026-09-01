package in.sapphirus.rupee.market.service;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class SymbolSyncJob {

    private static final Logger log = LoggerFactory.getLogger(SymbolSyncJob.class);

    private final JdbcTemplate jdbcTemplate;

    public SymbolSyncJob(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Daily scheduled background job executing at 7:00 AM IST (Asia/Kolkata).
     */
    @Scheduled(cron = "0 0 7 * * *", zone = "Asia/Kolkata")
    public void scheduleSync() {
        log.info("Starting scheduled Symbol Master Sync background job at 7:00 AM IST.");
        try {
            int count = syncSymbolsNow();
            log.info("Scheduled Symbol Master Sync completed. Upserted {} symbols.", count);
        } catch (Exception e) {
            log.error("Scheduled Symbol Master Sync failed", e);
        }
    }

    /**
     * Executes the symbol download, mapping, bulk database upsert, and delisting update.
     */
    @Transactional
    public int syncSymbolsNow() throws Exception {
        String csvUrl = "https://archives.nseindia.com/content/equities/EQUITY_L.csv";
        log.info("Downloading Symbol Master CSV from URL: {}", csvUrl);

        HttpURLConnection connection = (HttpURLConnection) new URL(csvUrl).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(15000);
        
        // Critical: Bypass NSE 403 blocks by specifying browser-like User-Agent header
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36");
        connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");

        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new RuntimeException("Failed to download CSV from NSE. HTTP Response: " + responseCode);
        }

        List<Object[]> batchArgs = new ArrayList<>();
        Set<String> activeSymbols = new HashSet<>();

        try (CSVParser parser = CSVParser.parse(
                new InputStreamReader(connection.getInputStream()),
                CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build())) {

            Map<String, Integer> headerMap = parser.getHeaderMap();
            log.info("CSV Headers detected: {}", headerMap.keySet());

            for (CSVRecord rec : parser) {
                String symbol = getRecordVal(rec, headerMap, "SYMBOL");
                if (symbol == null || symbol.trim().isEmpty()) {
                    continue;
                }

                String companyName = getRecordVal(rec, headerMap, "NAME OF COMPANY");
                String series = getRecordVal(rec, headerMap, "SERIES");
                String dateOfListing = getRecordVal(rec, headerMap, "DATE OF LISTING");
                String isin = getRecordVal(rec, headerMap, "ISIN NUMBER");

                String dbSymbol = "NSE:" + symbol.trim();
                activeSymbols.add(dbSymbol);

                LocalDate listingDate = null;
                if (dateOfListing != null && !dateOfListing.trim().isEmpty()) {
                    try {
                        listingDate = LocalDate.parse(dateOfListing.trim(), DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH));
                    } catch (Exception e) {
                        log.debug("Skip parsing listing date '{}' for symbol '{}'", dateOfListing, symbol);
                    }
                }

                batchArgs.add(new Object[] {
                        dbSymbol, // symbol
                        companyName != null ? companyName.trim() : "", // company_name
                        symbol.trim(), // short_name
                        isin != null ? isin.trim() : null, // isin
                        series != null ? series.trim() : null, // series
                        listingDate // listing_date
                });
            }
        }

        if (batchArgs.isEmpty()) {
            log.warn("No symbols parsed from downloaded CSV stream.");
            return 0;
        }

        log.info("Upserting {} symbols into the symbols database table.", batchArgs.size());

        // Perform batch upsert
        String upsertSql = "INSERT INTO symbols (symbol, company_name, short_name, exchange, instrument_type, isin, series, listing_date, is_active, last_synced_at, created_at, updated_at) " +
                "VALUES (?, ?, ?, 'NSE', 'EQ', ?, ?, ?, true, NOW(), NOW(), NOW()) " +
                "ON CONFLICT (symbol) DO UPDATE SET " +
                "  company_name = EXCLUDED.company_name, " +
                "  short_name = COALESCE(EXCLUDED.short_name, symbols.short_name), " +
                "  isin = EXCLUDED.isin, " +
                "  series = EXCLUDED.series, " +
                "  listing_date = EXCLUDED.listing_date, " +
                "  is_active = true, " +
                "  last_synced_at = NOW(), " +
                "  updated_at = NOW()";

        jdbcTemplate.batchUpdate(upsertSql, batchArgs);
        log.info("Batch upsert completed.");

        // Mark missing symbols as inactive (delisted)
        log.info("Marking delisted symbols as inactive...");
        int deactivatedCount = deactivateMissingSymbols(activeSymbols);
        log.info("Deactivated {} delisted symbols.", deactivatedCount);

        return batchArgs.size();
    }

    private String getRecordVal(CSVRecord rec, Map<String, Integer> headerMap, String key) {
        for (Map.Entry<String, Integer> entry : headerMap.entrySet()) {
            if (entry.getKey().trim().equalsIgnoreCase(key)) {
                return rec.get(entry.getValue());
            }
        }
        return null;
    }

    private int deactivateMissingSymbols(Set<String> activeSymbols) {
        if (activeSymbols.isEmpty()) {
            return 0;
        }

        // Since PostgreSQL easily handles 30,000+ placeholders, and active listings are ~2100, we can run it in a single SQL update safely.
        StringBuilder sb = new StringBuilder();
        List<String> list = new ArrayList<>(activeSymbols);
        for (int i = 0; i < list.size(); i++) {
            sb.append("?");
            if (i < list.size() - 1) {
                sb.append(",");
            }
        }

        String sql = "UPDATE symbols SET is_active = false, updated_at = NOW() " +
                "WHERE exchange = 'NSE' AND is_active = true AND symbol NOT IN (" + sb.toString() + ")";

        int totalUpdated = jdbcTemplate.update(sql, list.toArray());
        return totalUpdated;
    }
}
