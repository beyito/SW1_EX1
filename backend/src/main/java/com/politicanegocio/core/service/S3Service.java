package com.politicanegocio.core.service;

import com.politicanegocio.core.dto.UploadResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.Objects;

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

    public UploadResponseDto upload(
            MultipartFile file,
            String clientId,
            String processInstanceId,
            String documentId
    ) throws IOException {
        String safeFileName = sanitizeFileName(file.getOriginalFilename());
        String key = buildKey(clientId, processInstanceId, documentId, safeFileName);

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

    public String createPresignedReadUrl(String key) {
        return createPresignedReadUrl(key, presignedDuration);
    }

    public String createPresignedReadUrl(String key, Duration duration) {
        var getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        var presignedRequest = GetObjectPresignRequest.builder()
                .signatureDuration(duration == null ? presignedDuration : duration)
                .getObjectRequest(getObjectRequest)
                .build();

        return s3Presigner.presignGetObject(presignedRequest).url().toString();
    }

    public void replaceObject(String key, byte[] content, String contentType) {
        var putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .build();
        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(content));
    }

    public byte[] getObjectBytes(String key) {
        var getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();
        ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(getObjectRequest);
        return objectBytes.asByteArray();
    }

    public void deleteObject(String key) {
        var deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();
        s3Client.deleteObject(deleteObjectRequest);
    }

    private String buildKey(String clientId, String processInstanceId, String documentId, String fileName) {
        String cleanClientId = sanitizePathChunk(fallback(clientId, "anon"));
        String cleanProcessInstanceId = sanitizePathChunk(fallback(processInstanceId, "general"));
        String cleanDocumentId = sanitizePathChunk(fallback(documentId, "document"));
        return "clientes/" + cleanClientId + "/tramites/" + cleanProcessInstanceId + "/" + cleanDocumentId + "_" + fileName;
    }

    private String fallback(String value, String defaultValue) {
        String safe = Objects.toString(value, "").trim();
        return safe.isBlank() ? defaultValue : safe;
    }

    private String sanitizePathChunk(String value) {
        if (value == null || value.isBlank()) {
            return "general";
        }
        return value.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "file.bin";
        }
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
