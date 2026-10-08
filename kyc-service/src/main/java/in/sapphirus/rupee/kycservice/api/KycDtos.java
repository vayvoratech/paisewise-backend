package in.sapphirus.rupee.kycservice.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

public class KycDtos {

    @Data
    public static class InitKycResponse {
        private String authorizationUrl;
    }

    @Data
    public static class VerifyKycRequest {
        @NotNull(message = "User ID is required")
        private Long userId;

        @NotBlank(message = "Aadhaar number is required")
        @Pattern(regexp = "^\\d{12}$", message = "Aadhaar must be a 12-digit number")
        private String fullAadhaar;

        @NotBlank(message = "PAN number is required")
        @Pattern(regexp = "[A-Z]{5}[0-9]{4}[A-Z]{1}", message = "Invalid PAN format")
        private String pan;

        @NotBlank(message = "Provided full name is required")
        private String fullName;

        @NotBlank(message = "Verified document name is required")
        private String verifiedName;
    }

    @Data
    public static class KycResponseDto {
        private Long userId;
        private String aadhaarMasked;
        private double nameMatchPercentage;
        private boolean verified;
        private String message;
    }

    @Data
    public static class VideoKycInitRequest {
        @NotNull(message = "User ID is required")
        private Long userId;

        @NotBlank(message = "Customer full name is required")
        private String name;

        @NotBlank(message = "Customer email is required")
        private String email;

        @NotBlank(message = "Customer mobile number is required")
        @Pattern(regexp = "^\\d{10}$", message = "Mobile number must be 10 digits")
        private String mobileNumber;
    }

    @Data
    public static class VideoKycInitResponse {
        private Long userId;
        private String referenceId;
        private String webViewUrl;
        private String status;
    }

    @Data
    public static class VideoKycWebhookPayload {
        @NotBlank(message = "Reference ID is required")
        private String referenceId;

        @NotNull(message = "User ID is required")
        private Long userId;

        @NotBlank(message = "Status is required")
        private String status; // SUCCESS, FAILED, REJECTED

        private String reason;
    }
}