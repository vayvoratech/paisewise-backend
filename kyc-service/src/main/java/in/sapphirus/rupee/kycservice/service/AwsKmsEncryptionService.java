package in.sapphirus.rupee.kycservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.DecryptRequest;
import software.amazon.awssdk.services.kms.model.DecryptResponse;
import software.amazon.awssdk.services.kms.model.EncryptRequest;
import software.amazon.awssdk.services.kms.model.EncryptResponse;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class AwsKmsEncryptionService {

    private final KmsClient kmsClient;

    @Value("${cloud.aws.kms.key-id}")
    private String kmsKeyId;

    private static final String AES_ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int IV_LENGTH_BYTE = 12;

    public EncryptedData encryptPan(String plainPan) {
        try {
            // 1. Generate local data key for AES-256-GCM
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(256);
            SecretKey secretKey = keyGen.generateKey();

            // 2. Encrypt PAN string using AES-256-GCM
            byte[] iv = new byte[IV_LENGTH_BYTE];
            SecureRandom.getInstanceStrong().nextBytes(iv);
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            byte[] cipherTextWithTag = cipher.doFinal(plainPan.getBytes(StandardCharsets.UTF_8));

            // Extract Tag and Ciphertext if required, or keep them bundled depending on standard output
            // Standard Cipher.doFinal returns cipher text appended with authentication tag in Java.
            String encodedCipherText = Base64.getEncoder().encodeToString(cipherTextWithTag);
            String encodedIv = Base64.getEncoder().encodeToString(iv);

            // 3. Encrypt the local data key via AWS KMS
            EncryptRequest encryptRequest = EncryptRequest.builder()
                    .keyId(kmsKeyId)
                    .plaintext(SdkBytes.fromByteArray(secretKey.getEncoded()))
                    .build();
            EncryptResponse encryptResponse = kmsClient.encrypt(encryptRequest);
            String encryptedDataKey = Base64.getEncoder().encodeToString(encryptResponse.ciphertextBlob().asByteArray());

            return new EncryptedData(encodedCipherText, encodedIv, encryptedDataKey);
        } catch (Exception e) {
            throw new RuntimeException("Error encrypting PAN data via AWS KMS", e);
        }
    }

    public record EncryptedData(String encryptedText, String iv, String encryptedDataKey) {}
}