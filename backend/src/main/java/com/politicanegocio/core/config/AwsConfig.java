package com.politicanegocio.core.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class AwsConfig {

    @Bean
    public Region awsRegion(@Value("${aws.region}") String region) {
        return Region.of(region);
    }

    @Bean
    public S3Client s3Client(
            Region awsRegion,
            @Value("${aws.access-key-id:}") String accessKeyId,
            @Value("${aws.secret-access-key:}") String secretAccessKey
    ) {
        if (!accessKeyId.isBlank() && !secretAccessKey.isBlank()) {
            var credentials = AwsBasicCredentials.create(accessKeyId, secretAccessKey);
            return S3Client.builder()
                    .region(awsRegion)
                    .credentialsProvider(StaticCredentialsProvider.create(credentials))
                    .build();
        }

        return S3Client.builder()
                .region(awsRegion)
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @Bean
    public S3Presigner s3Presigner(
            Region awsRegion,
            @Value("${aws.access-key-id:}") String accessKeyId,
            @Value("${aws.secret-access-key:}") String secretAccessKey
    ) {
        if (!accessKeyId.isBlank() && !secretAccessKey.isBlank()) {
            var credentials = AwsBasicCredentials.create(accessKeyId, secretAccessKey);
            return S3Presigner.builder()
                    .region(awsRegion)
                    .credentialsProvider(StaticCredentialsProvider.create(credentials))
                    .build();
        }

        return S3Presigner.builder()
                .region(awsRegion)
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
}
