package in.sapphirus.rupee.portfolio.service;

import in.sapphirus.rupee.portfolio.domain.MfScheme;
import in.sapphirus.rupee.portfolio.repo.MfSchemeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiFundRecommendationService Tests")
class AiFundRecommendationServiceTest {

    @Mock
    private MfSchemeRepository schemeRepo;

    private AiFundRecommendationService aiService;

    @BeforeEach
    void setUp() {
        aiService = new AiFundRecommendationService(schemeRepo);
    }

    private MfScheme createScheme(String code, String name, String amc, String cat, String risk, double r3, double er, boolean isTaxSaver) {
        MfScheme s = new MfScheme(code, name, amc, cat, 100.0);
        s.setRiskLevel(risk);
        s.setReturns3y(r3);
        s.setReturns1y(18.0);
        s.setReturns5y(20.0);
        s.setExpenseRatio(er);
        s.setFundSizeCr(25000.0);
        s.setTaxSaver(isTaxSaver);
        s.setActive(true);
        return s;
    }

    @Test
    @DisplayName("generates recommendations for TAX_SAVING goal focusing on ELSS")
    void recommendations_taxSavingGoal() {
        MfScheme elss = createScheme("101", "Canara Robeco ELSS", "Canara Robeco", "ELSS", "Very High", 22.0, 0.58, true);
        MfScheme largeCap = createScheme("102", "Mirae Large Cap", "Mirae", "Large Cap", "Very High", 18.0, 0.60, false);

        when(schemeRepo.findAll()).thenReturn(List.of(elss, largeCap));

        AiFundRecommendationService.RecommendationRequest req = new AiFundRecommendationService.RecommendationRequest(
                "HIGH", "TAX_SAVING", 3, 5000.0, null, "en"
        );

        AiFundRecommendationService.RecommendationResponse resp = aiService.generateRecommendations(req);

        assertThat(resp.recommendations()).isNotEmpty();
        assertThat(resp.recommendations().get(0).schemeCode()).isEqualTo("101");
        assertThat(resp.recommendations().get(0).isTaxSaver()).isTrue();
        assertThat(resp.recommendations().get(0).aiScore()).isGreaterThanOrEqualTo(80);
        assertThat(resp.assetAllocation()).isNotEmpty();
    }

    @Test
    @DisplayName("generates Hindi explanations when language is 'hi'")
    void recommendations_hindiLanguage() {
        MfScheme flexiCap = createScheme("103", "Parag Parikh Flexi Cap", "PPFAS", "Flexi Cap", "Very High", 24.0, 0.62, false);

        when(schemeRepo.findAll()).thenReturn(List.of(flexiCap));

        AiFundRecommendationService.RecommendationRequest req = new AiFundRecommendationService.RecommendationRequest(
                "HIGH", "WEALTH_CREATION", 5, 10000.0, null, "hi"
        );

        AiFundRecommendationService.RecommendationResponse resp = aiService.generateRecommendations(req);

        assertThat(resp.recommendations()).isNotEmpty();
        assertThat(resp.recommendations().get(0).aiReasonHi()).isNotNull();
        assertThat(resp.recommendations().get(0).aiReasonHi()).contains("रिटर्न");
        assertThat(resp.profileSummary()).contains("प्रोफ़ाइल");
    }
}
