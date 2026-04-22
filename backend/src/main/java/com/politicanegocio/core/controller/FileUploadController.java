package com.politicanegocio.core.controller;

import com.politicanegocio.core.dto.UploadResponseDto;
import com.politicanegocio.core.service.S3Service;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
public class FileUploadController {

    private final S3Service s3Service;

    public FileUploadController(S3Service s3Service) {
        this.s3Service = s3Service;
    }

    @PostMapping("/upload")
    public ResponseEntity<UploadResponseDto> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "policyId", required = false) String policyId
    ) throws Exception {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(s3Service.upload(file, policyId));
    }
}
