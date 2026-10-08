package in.sapphirus.rupee.portfolio.service;

import in.sapphirus.rupee.portfolio.domain.MfScheme;
import in.sapphirus.rupee.portfolio.repo.MfSchemeRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * High-level service for Mutual Fund Scheme catalog, searching, filtering, and AI recommendations.
 */
@Service
public class FundService {

    private static final Logger log = LoggerFactory.getLogger(FundService.class);

    private final MfSchemeRepository schemeRepo;
    private final AiFundRecommendationService aiAdvisor;

    public FundService(MfSchemeRepository schemeRepo, AiFundRecommendationService aiAdvisor) {
        this.schemeRepo = schemeRepo;
        this.aiAdvisor = aiAdvisor;
    }

    @PostConstruct
    public void init() {
        seedInitialSchemesIfEmpty();
    }

    public record SchemeFilter(
            String query,
            String category,
            String amc,
            String riskLevel,
            Boolean isTaxSaver,
            Double minSipMax,
            String sortBy, // RETURNS_3Y, RETURNS_1Y, NAV, EXPENSE_RATIO, NAME
            int page,
            int size
    ) {}

    public record SchemeSummaryView(
            String schemeCode,
            String isin,
            String schemeName,
            String amcName,
            String category,
            String subCategory,
            String riskLevel,
            double nav,
            LocalDate navDate,
            double minSipAmount,
            double minLumpsum,
            Double returns1y,
            Double returns3y,
            Double returns5y,
            Double expenseRatio,
            Double fundSizeCr,
            boolean isTaxSaver,
            String bseSchemeCode
    ) {
        public static SchemeSummaryView from(MfScheme s) {
            return new SchemeSummaryView(
                    s.getSchemeCode(),
                    s.getIsin(),
                    s.getSchemeName(),
                    s.getAmcName(),
                    s.getCategory(),
                    s.getSubCategory(),
                    s.getRiskLevel(),
                    s.getNav(),
                    s.getNavDate(),
                    s.getMinSipAmount(),
                    s.getMinLumpsum(),
                    s.getReturns1y(),
                    s.getReturns3y(),
                    s.getReturns5y(),
                    s.getExpenseRatio(),
                    s.getFundSizeCr(),
                    s.isTaxSaver(),
                    s.getBseSchemeCode()
            );
        }
    }

    /**
     * Search schemes with full text query and multiple filters.
     */
    public Page<SchemeSummaryView> searchSchemes(SchemeFilter filter) {
        List<MfScheme> results;
        if (filter.query() != null && !filter.query().isBlank()) {
            results = schemeRepo.searchSchemes(filter.query().trim());
        } else {
            results = schemeRepo.findAll().stream().filter(MfScheme::isActive).collect(Collectors.toList());
        }

        // Apply filters in-memory for fast and dynamic search
        List<MfScheme> filtered = results.stream()
                .filter(s -> filter.category() == null || filter.category().isBlank() ||
                        (s.getCategory() != null && s.getCategory().equalsIgnoreCase(filter.category().trim())))
                .filter(s -> filter.amc() == null || filter.amc().isBlank() ||
                        (s.getAmcName() != null && s.getAmcName().equalsIgnoreCase(filter.amc().trim())))
                .filter(s -> filter.riskLevel() == null || filter.riskLevel().isBlank() ||
                        (s.getRiskLevel() != null && s.getRiskLevel().equalsIgnoreCase(filter.riskLevel().trim())))
                .filter(s -> filter.isTaxSaver() == null || s.isTaxSaver() == filter.isTaxSaver())
                .filter(s -> filter.minSipMax() == null || s.getMinSipAmount() <= filter.minSipMax())
                .collect(Collectors.toList());

        // Apply Sorting
        Comparator<MfScheme> comparator = Comparator.comparing(
                MfScheme::getReturns3y,
                Comparator.nullsLast(Comparator.reverseOrder())
        );

        if ("RETURNS_1Y".equalsIgnoreCase(filter.sortBy())) {
            comparator = Comparator.comparing(MfScheme::getReturns1y, Comparator.nullsLast(Comparator.reverseOrder()));
        } else if ("NAV".equalsIgnoreCase(filter.sortBy())) {
            comparator = Comparator.comparing(MfScheme::getNav, Comparator.reverseOrder());
        } else if ("EXPENSE_RATIO".equalsIgnoreCase(filter.sortBy())) {
            comparator = Comparator.comparing(MfScheme::getExpenseRatio, Comparator.nullsLast(Comparator.naturalOrder()));
        } else if ("NAME".equalsIgnoreCase(filter.sortBy())) {
            comparator = Comparator.comparing(MfScheme::getSchemeName, String.CASE_INSENSITIVE_ORDER);
        }

        filtered.sort(comparator);

        // Pagination
        int page = Math.max(0, filter.page());
        int size = filter.size() > 0 ? filter.size() : 20;
        int fromIndex = Math.min(page * size, filtered.size());
        int toIndex = Math.min(fromIndex + size, filtered.size());

        List<SchemeSummaryView> pagedList = filtered.subList(fromIndex, toIndex).stream()
                .map(SchemeSummaryView::from)
                .toList();

        return new PageImpl<>(pagedList, PageRequest.of(page, size), filtered.size());
    }

    /**
     * Get scheme details by code or ISIN.
     */
    public Optional<MfScheme> getScheme(String schemeCodeOrIsin) {
        Optional<MfScheme> byCode = schemeRepo.findById(schemeCodeOrIsin);
        if (byCode.isPresent()) {
            return byCode;
        }
        return schemeRepo.findByIsin(schemeCodeOrIsin);
    }

    /**
     * Get top trending / high return mutual funds.
     */
    public List<SchemeSummaryView> getTrendingFunds(int limit) {
        int count = limit > 0 ? limit : 6;
        return schemeRepo.findTop10ByIsActiveTrueOrderByReturns3yDesc().stream()
                .limit(count)
                .map(SchemeSummaryView::from)
                .toList();
    }

    /**
     * Get list of unique fund categories.
     */
    public List<String> getCategories() {
        return schemeRepo.findDistinctCategories();
    }

    /**
     * Get list of unique AMCs (Asset Management Companies).
     */
    public List<String> getAmcs() {
        return schemeRepo.findDistinctAmcs();
    }

    /**
     * Get personalized AI fund recommendations.
     */
    public AiFundRecommendationService.RecommendationResponse getAiRecommendations(
            AiFundRecommendationService.RecommendationRequest request) {
        return aiAdvisor.generateRecommendations(request);
    }

    /**
     * Pre-populates the database with initial popular mutual funds if the catalog is empty.
     */
    @Transactional
    public void seedInitialSchemesIfEmpty() {
        if (schemeRepo.count() > 0) {
            return;
        }

        log.info("Seeding initial curated mutual fund catalog...");
        List<MfScheme> initialList = List.of(
                createScheme("122639", "INF846K01164", "Parag Parikh Flexi Cap Fund - Direct Plan - Growth",
                        "PPFAS Mutual Fund", "Flexi Cap Fund", "Equity Scheme", "Very High", 82.45,
                        1000.0, 1000.0, 24.5, 21.8, 22.4, 0.62, "Rajeev Thakkar", 68500.0, false, 0),

                createScheme("120503", "INF209K01167", "Mirae Asset Large Cap Fund - Direct Plan - Growth",
                        "Mirae Asset Mutual Fund", "Large Cap Fund", "Equity Scheme", "Very High", 118.32,
                        500.0, 1000.0, 19.8, 16.5, 17.2, 0.54, "Gaurav Misra", 37400.0, false, 0),

                createScheme("120716", "INF760C01018", "Canara Robeco ELSS Tax Saver - Direct Plan - Growth",
                        "Canara Robeco Mutual Fund", "ELSS", "Equity Scheme", "Very High", 164.20,
                        500.0, 500.0, 22.1, 19.4, 18.9, 0.58, "Shridatta Bhandwaldar", 8100.0, true, 3),

                createScheme("125497", "INF204KB1882", "Quant Small Cap Fund - Direct Plan - Growth",
                        "Quant Mutual Fund", "Small Cap Fund", "Equity Scheme", "Very High", 248.90,
                        1000.0, 5000.0, 38.4, 32.1, 36.8, 0.77, "Sanjeev Sharma", 21200.0, false, 0),

                createScheme("120586", "INF179K01BE2", "HDFC Top 100 Fund - Direct Plan - Growth",
                        "HDFC Mutual Fund", "Large Cap Fund", "Equity Scheme", "Very High", 1024.15,
                        500.0, 1000.0, 23.2, 18.7, 16.8, 0.98, "Rahul Baijal", 34100.0, false, 0),

                createScheme("120251", "INF109K01014", "ICICI Prudential Bluechip Fund - Direct Plan - Growth",
                        "ICICI Prudential Mutual Fund", "Large Cap Fund", "Equity Scheme", "Very High", 104.85,
                        100.0, 500.0, 21.4, 17.9, 16.5, 0.91, "Anish Tawakley", 54300.0, false, 0),

                createScheme("119551", "INF200K01VA0", "SBI Bluechip Fund - Direct Plan - Growth",
                        "SBI Mutual Fund", "Large Cap Fund", "Equity Scheme", "Very High", 89.60,
                        500.0, 5000.0, 18.2, 15.6, 15.1, 0.85, "Sohini Andani", 46200.0, false, 0),

                createScheme("120823", "INF209KA1130", "Mirae Asset Midcap Fund - Direct Plan - Growth",
                        "Mirae Asset Mutual Fund", "Mid Cap Fund", "Equity Scheme", "Very High", 34.12,
                        500.0, 1000.0, 28.7, 24.3, 23.9, 0.61, "Ankit Jain", 16400.0, false, 0),

                createScheme("120302", "INF109K01121", "ICICI Prudential Liquid Fund - Direct Plan - Growth",
                        "ICICI Prudential Mutual Fund", "Liquid Fund", "Debt Scheme", "Low to Moderate", 365.12,
                        500.0, 1000.0, 7.2, 6.4, 5.9, 0.20, "Darshil Pandya", 48000.0, false, 0)
        );

        schemeRepo.saveAll(initialList);
        log.info("Successfully seeded {} mutual fund schemes.", initialList.size());
    }

    private MfScheme createScheme(String code, String isin, String name, String amc, String cat, String subCat,
                                  String risk, double nav, double minSip, double minLump, Double r1, Double r3,
                                  Double r5, Double er, String manager, Double aumCr, boolean taxSaver, int lockIn) {
        MfScheme s = new MfScheme(code, name, amc, cat, nav);
        s.setIsin(isin);
        s.setSubCategory(subCat);
        s.setRiskLevel(risk);
        s.setNavDate(LocalDate.now());
        s.setMinSipAmount(minSip);
        s.setMinLumpsum(minLump);
        s.setReturns1y(r1);
        s.setReturns3y(r3);
        s.setReturns5y(r5);
        s.setExpenseRatio(er);
        s.setFundManager(manager);
        s.setFundSizeCr(aumCr);
        s.setTaxSaver(taxSaver);
        s.setLockInYears(lockIn);
        s.setBseSchemeCode("BSE_" + code);
        s.setNseSymbol("MF_" + code);
        return s;
    }
}
