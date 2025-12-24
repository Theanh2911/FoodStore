package yenha.foodstore.Config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class S3Config {

    @Value("${AWS_ACCESS_KEY_ID}")
    private String accessKey;
    
    @Value("${AWS_SECRET_ACCESS_KEY}")
    private String secretKey;
    
    @Value("${AWS_REGION}")
    private String awsRegion;

    @Bean
    public S3Client s3Client() {
        if (accessKey == null || accessKey.isEmpty()) {
            throw new IllegalStateException("AWS_ACCESS_KEY_ID must be set in .env file");
        }
        if (secretKey == null || secretKey.isEmpty()) {
            throw new IllegalStateException("AWS_SECRET_ACCESS_KEY must be set in .env file");
        }
        if (awsRegion == null || awsRegion.isEmpty()) {
            throw new IllegalStateException("AWS_REGION must be set in .env file");
        }

        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);

        return S3Client.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
    }
}


