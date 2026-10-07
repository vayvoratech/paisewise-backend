package in.sapphirus.rupee.portfolio.service;

import in.sapphirus.rupee.portfolio.domain.MfScheme;
import in.sapphirus.rupee.portfolio.repo.MfSchemeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FundService Tests")
class FundServiceTest {

    @Mock
    private MfSchemeRepository schemeRepo;

    @Mock
    private AiFundRecommendationService aiAdvisor;

    private FundService fundService;

    @BeforeEach
    void setUp() {
        fundService = new FundService(schemeRepo, aiAdvisor);
    }

    private MfScheme sampleScheme(String code, String name, String amc, String cat, double nav, Double r3, boolean isTaxSaver) {
        MfScheme s = new MfScheme(code, name, amc, cat, nav);
        s.setReturns3y(r3);
        s.setReturns1y(15.0);
        s.setExpenseRatio(0.75);
        s.setMinSipAmount(500.0);
        s.setTaxSaver(isTaxSaver);
        s.setActive(true);
        return s;
    }

    @Test
    @DisplayName("searchSchemes returns paginated filtered results")
    void searchSchemes_withFilters() {
        MfScheme s1 = sampleScheme("1001", "Parag Parikh Flexi Cap", "PPFAS Mutual Fund", "Flexi Cap", 80.0, 24.0, false);
        MfScheme s2 = sampleScheme("1002", "Mirae Asset Large Cap", "Mirae Asset", "Large Cap", 110.0, 18.0, false);
        MfScheme s3 = sampleScheme("1003", "Canara Robeco ELSS Tax Saver", "Canara Robeco", "ELSS", 150.0, 20.0, true);

        when(schemeRepo.findAll()).thenReturn(List.of(s1, s2, s3));

        FundService.SchemeFilter filter = new FundService.SchemeFilter(
                null, "Flexi Cap", null, null, null, null, "RETURNS_3Y", 0, 10
        );

        Page<FundService.SchemeSummaryView> page = fundService.searchSchemes(filter);

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).schemeCode()).isEqualTo("1001");
        assertThat(page.getContent().get(0).schemeName()).contains("Parag Parikh");
    }

    @Test
    @DisplayName("searchSchemes with text query uses repository search")
    void searchSchemes_withQuery() {
        MfScheme s1 = sampleScheme("1001", "Parag Parikh Flexi Cap", "PPFAS Mutual Fund", "Flexi Cap", 80.0, 24.0, false);
        when(schemeRepo.searchSchemes(anyString())).thenReturn(List.of(s1));

        FundService.SchemeFilter filter = new FundService.SchemeFilter(
                "Parag", null, null, null, null, null, "RETURNS_3Y", 0, 10
        );

        Page<FundService.SchemeSummaryView> page = fundService.searchSchemes(filter);

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).schemeCode()).isEqualTo("1001");
    }

    @Test
    @DisplayName("getScheme returns scheme by code or ISIN")
    void getScheme_byCodeOrIsin() {
        MfScheme s1 = sampleScheme("1001", "Parag Parikh Flexi Cap", "PPFAS Mutual Fund", "Flexi Cap", 80.0, 24.0, false);
        s1.setIsin("INF846K01164");

        when(schemeRepo.findById("1001")).thenReturn(Optional.of(s1));
        when(schemeRepo.findById("INF846K01164")).thenReturn(Optional.empty());
        when(schemeRepo.findByIsin("INF846K01164")).thenReturn(Optional.of(s1));

        Optional<MfScheme> byCode = fundService.getScheme("1001");
        Optional<MfScheme> byIsin = fundService.getScheme("INF846K01164");

        assertThat(byCode).isPresent();
        assertThat(byCode.get().getSchemeCode()).isEqualTo("1001");
        assertThat(byIsin).isPresent();
        assertThat(byIsin.get().getIsin()).isEqualTo("INF846K01164");
    }

    @Test
    @DisplayName("getCategories and getAmcs return distinct lists")
    void getCategoriesAndAmcs() {
        when(schemeRepo.findDistinctCategories()).thenReturn(List.of("ELSS", "Flexi Cap", "Large Cap"));
        when(schemeRepo.findDistinctAmcs()).thenReturn(List.of("HDFC", "ICICI", "SBI"));

        List<String> categories = fundService.getCategories();
        List<String> amcs = fundService.getAmcs();

        assertThat(categories).containsExactly("ELSS", "Flexi Cap", "Large Cap");
        assertThat(amcs).containsExactly("HDFC", "ICICI", "SBI");
    }
}
