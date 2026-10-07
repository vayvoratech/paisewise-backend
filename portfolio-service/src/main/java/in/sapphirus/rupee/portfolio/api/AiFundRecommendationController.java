package in.sapphirus.rupee.portfolio.api;

import in.sapphirus.rupee.portfolio.service.AiFundRecommendationService;
import in.sapphirus.rupee.portfolio.service.AiFundRecommendationService.FundRecommendRequest;
import in.sapphirus.rupee.security.CurrentUser;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/portfolio/ai")
public class AiFundRecommendationController {

    private final AiFundRecommendationService aiFundService;

    public AiFundRecommendationController(AiFundRecommendationService aiFundService) {
        this.aiFundService = aiFundService;
    }

    @PostMapping("/fund-recommend")
    public Map<String, Object> recommendFunds(@RequestBody(required = false) FundRecommendRequest request) {
        String activeUserId = CurrentUser.id() != null ? CurrentUser.id() : "1";
        if (request == null) {
            request = new FundRecommendRequest(activeUserId, "moderate", 50000.0, 5, "wealth creation", "English");
        } else if (request.userId() == null || request.userId().isEmpty() || "me".equalsIgnoreCase(request.userId())) {
            request = new FundRecommendRequest(activeUserId, request.riskProfile(), request.investmentAmount(), request.investmentHorizon(), request.userGoal(), request.language());
        }
        return aiFundService.getRecommendations(request);
    }
}
