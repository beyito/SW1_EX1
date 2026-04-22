package com.politicanegocio.core.service;

import com.politicanegocio.core.dto.UploadResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class S3Service {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucket;
    private final Duration presignedDuration;

    public S3Service(
            S3Client s3Client,
            S3Presigner s3Presigner,
            @Value("${aws.s3.bucket}") String bucket,
            @Value("${aws.s3.presigned-expiration-minutes:10080}") long presignedMinutes
    ) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.bucket = bucket;
        this.presignedDuration = Duration.ofMinutes(Math.max(1, presignedMinutes));
    }

    public UploadResponseDto upload(MultipartFile file, String policyId) throws IOException {
        String safeFileName = sanitizeFileName(file.getOriginalFilename());
        String key = buildKey(policyId, safeFileName);

        var putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(file.getContentType())
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));

        var getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        var presignedRequest = GetObjectPresignRequest.builder()
                .signatureDuration(presignedDuration)
                .getObjectRequest(getObjectRequest)
                .build();

        String url = s3Presigner.presignGetObject(presignedRequest).url().toString();
        return new UploadResponseDto(key, url, file.getContentType(), file.getSize());
    }

    private String buildKey(String policyId, String fileName) {
        String cleanPolicyId = (policyId == null || policyId.isBlank()) ? "general" : policyId.trim();
        String timestamp = Instant.now().toString().replace(':', '-');
        return "policies/" + cleanPolicyId + "/attachments/" + timestamp + "-" + UUID.randomUUID() + "-" + fileName;
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "file.bin";
        }
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
