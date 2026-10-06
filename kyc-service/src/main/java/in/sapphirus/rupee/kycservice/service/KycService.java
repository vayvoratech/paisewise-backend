package in.sapphirus.rupee.kycservice.service;

import in.sapphirus.rupee.kycservice.api.KycDtos;
import in.sapphirus.rupee.kycservice.domain.KycRecord;
import in.sapphirus.rupee.kycservice.repo.KycRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class KycService {

    private final KycRepository kycRepository;
    private final AwsKmsEncryptionService kmsEncryptionService;

    @Value("${digilocker.client-id}")
    private String clientId;

    @Value("${digilocker.redirect-uri}")
    private String redirectUri;

    @Value("${digilocker.auth-base-url}")
    private String authBaseUrl;

    public String initKyc() {
        return String.format("%s?response_type=code&client_id=%s&redirect_uri=%s",
                authBaseUrl, clientId, redirectUri);
    }

    @Transactional
    public KycDtos.KycResponseDto processKycVerification(KycDtos.VerifyKycRequest request) {
        // 1. Extract and save ONLY last 4 digits of Aadhaar (NEVER save full Aadhaar)
        String fullAadhaar = request.getFullAadhaar();
        if (fullAadhaar == null || fullAadhaar.length() < 4) {
            throw new IllegalArgumentException("Invalid Aadhaar number provided");
        }
        String aadhaarLastFour = fullAadhaar.substring(fullAadhaar.length() - 4);
        String maskedAadhaar = "XXXX-XXXX-" + aadhaarLastFour;

        // 2. Encrypt PAN using AWS KMS and AES-256-GCM
        AwsKmsEncryptionService.EncryptedData encryptedPanData =
                kmsEncryptionService.encryptPan(request.getPan());

        // 3. Name match check via Levenshtein fuzzy distance (>85%)
        double matchPercentage = calculateNameMatchPercentage(request.getFullName(), request.getVerifiedName());
        boolean isNameMatchValid = matchPercentage >= 85.0;

        // 4. Save to Database
        KycRecord kycRecord = KycRecord.builder()
                .userId(request.getUserId())
                .aadhaarLastFour(aadhaarLastFour)
                .encryptedPan(encryptedPanData.encryptedText())
                .panIv(encryptedPanData.iv())
                .panAuthTag(encryptedPanData.encryptedDataKey()) // Storing wrapped data key reference
                .fullName(request.getFullName())
                .verifiedName(request.getVerifiedName())
                .nameMatchPercentage(matchPercentage)
                .verified(isNameMatchValid)
                .build();

        kycRepository.save(kycRecord);

        KycDtos.KycResponseDto response = new KycDtos.KycResponseDto();
        response.setUserId(request.getUserId());
        response.setAadhaarMasked(maskedAadhaar);
        response.setNameMatchPercentage(matchPercentage);
        response.setVerified(isNameMatchValid);
        response.setMessage(isNameMatchValid ? "KYC completed successfully." : "KYC failed: Name match score is below 85%.");

        return response;
    }

    private double calculateNameMatchPercentage(String name1, String name2) {
        if (name1 == null || name2 == null) {
            return 0.0;
        }
        String s1 = name1.trim().toLowerCase();
        String s2 = name2.trim().toLowerCase();

        int maxLen = Math.max(s1.length(), s2.length());
        if (maxLen == 0) {
            return 100.0;
        }

        LevenshteinDistance levenshteinDistance = new LevenshteinDistance();
        int distance = levenshteinDistance.apply(s1, s2);

        double similarity = ((double) (maxLen - distance) / maxLen) * 100.0;
        return Math.max(0.0, similarity);
    }
}