package in.sapphirus.rupee.kycservice.service;


import in.sapphirus.rupee.kycservice.api.KycDtos;
import in.sapphirus.rupee.kycservice.domain.KycRecord;
import in.sapphirus.rupee.kycservice.repo.KycRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoKycService {

    private final KycRepository kycRepository;
    private final RestTemplate restTemplate;

    @Value("${idfy.api.base-url:https://api.kyc.idfy.com}")
    private String idfyBaseUrl;

    @Value("${idfy.api.key:}")
    private String idfyApiKey;

    @Value("${idfy.account.id:}")
    private String idfyAccountId;

    @Transactional
    public KycDtos.VideoKycInitResponse initiateVideoKyc(KycDtos.VideoKycInitRequest request) {
        String referenceId = "VKYC-" + UUID.randomUUID();
        String url = idfyBaseUrl + "/v3/tasks/sync/interviewer-less/video-kyc";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("api-key", idfyApiKey);
        headers.set("account-id", idfyAccountId);

        Map<String, Object> body = Map.of(
                "reference_id", referenceId,
                "data", Map.of(
                        "customer_fname", request.getName(),
                        "customer_email", request.getEmail(),
                        "customer_phone", request.getMobileNumber()
                )
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        String webViewUrl;

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                // Extract provider webview/capture link safely
                webViewUrl = (String) responseBody.getOrDefault("url", "https://capture.idfy.com/fallback");
            } else {
                throw new IllegalStateException("Failed to initialize Video KYC session with third-party provider");
            }
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("Provider API error during Video KYC initialization: Status {}, Response {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("External Video KYC provider error: " + e.getStatusText(), e);
        } catch (Exception e) {
            log.error("Unexpected error calling Video KYC provider: {}", e.getMessage(), e);
            throw new RuntimeException("Internal error while setting up Video KYC session", e);
        }

        KycDtos.VideoKycInitResponse initResponse = new KycDtos.VideoKycInitResponse();
        initResponse.setUserId(request.getUserId());
        initResponse.setReferenceId(referenceId);
        initResponse.setWebViewUrl(webViewUrl);
        initResponse.setStatus("INITIATED");

        return initResponse;
    }

    @Transactional
    public void handleVideoCallback(KycDtos.VideoKycWebhookPayload webhookPayload) {
        log.info("Processing videoCallback webhook for userId: {} with referenceId: {} and status: {}",
                webhookPayload.getUserId(), webhookPayload.getReferenceId(), webhookPayload.getStatus());

        KycRecord record = kycRepository.findByUserId(webhookPayload.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("KYC record not found for user ID: " + webhookPayload.getUserId()));

        boolean isSuccess = "SUCCESS".equalsIgnoreCase(webhookPayload.getStatus()) ||
                "VERIFIED".equalsIgnoreCase(webhookPayload.getStatus());

        record.setVerified(isSuccess);
        kycRepository.save(record);

        // Publish event for downstream message queues (e.g., Kafka / RabbitMQ `kyc.events`)
        publishKycEvent(webhookPayload.getUserId(), webhookPayload.getStatus(), webhookPayload.getReferenceId());
    }

    private void publishKycEvent(Long userId, String status, String referenceId) {
        // Production implementation should dispatch via KafkaTemplate or RabbitTemplate
        log.info("Successfully published kyc.events -> [Topic: kyc.events] userId: {}, status: {}, refId: {}",
                userId, status, referenceId);
    }
}