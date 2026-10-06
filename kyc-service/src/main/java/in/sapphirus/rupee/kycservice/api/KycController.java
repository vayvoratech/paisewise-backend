package in.sapphirus.rupee.kycservice.api;

import in.sapphirus.rupee.kycservice.service.KycService;
import in.sapphirus.rupee.kycservice.api.KycDtos;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/kyc")
@RequiredArgsConstructor
public class KycController {

    private final KycService kycService;

    @GetMapping("/digilocker/init")
    public ResponseEntity<KycDtos.InitKycResponse> initKyc() {
        String authUrl = kycService.initKyc();
        KycDtos.InitKycResponse response = new KycDtos.InitKycResponse();
        response.setAuthorizationUrl(authUrl);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify")
    public ResponseEntity<KycDtos.KycResponseDto> verifyKyc(@Valid @RequestBody KycDtos.VerifyKycRequest request) {
        KycDtos.KycResponseDto result = kycService.processKycVerification(request);
        return ResponseEntity.ok(result);
    }
}