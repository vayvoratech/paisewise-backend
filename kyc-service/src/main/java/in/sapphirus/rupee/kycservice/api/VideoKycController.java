package in.sapphirus.rupee.kycservice.api;

import in.sapphirus.rupee.kycservice.service.VideoKycService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/kyc/video")
@RequiredArgsConstructor
public class VideoKycController {

    private final VideoKycService videoKycService;

    @PostMapping("/initiate")
    public ResponseEntity<KycDtos.VideoKycInitResponse> initiateVideoKyc(
            @Valid @RequestBody KycDtos.VideoKycInitRequest request) {
        KycDtos.VideoKycInitResponse response = videoKycService.initiateVideoKyc(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> videoCallback(
            @Valid @RequestBody KycDtos.VideoKycWebhookPayload webhookPayload) {
        videoKycService.handleVideoCallback(webhookPayload);
        return ResponseEntity.ok().build();
    }
}