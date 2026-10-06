package in.sapphirus.rupee.kycservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kms.KmsClient;

@Configuration
public class AwsKmsConfig {

    @Value("${cloud.aws.region.static:us-east-1}")
    private String awsRegion;

    @Bean
    public KmsClient kmsClient() {
        return KmsClient.builder()
                .region(Region.of(awsRegion))
                .build();
    }
}
