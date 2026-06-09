package com.politicanegocio.core.controller;

import com.politicanegocio.core.dto.DynamicReportPlanDto;
import com.politicanegocio.core.dto.DynamicReportRequestDto;
import com.politicanegocio.core.dto.GeneratedReportFileDto;
import com.politicanegocio.core.model.User;
import com.politicanegocio.core.service.DynamicReportService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports/dynamic")
public class DynamicReportController {
    private final DynamicReportService dynamicReportService;

    public DynamicReportController(DynamicReportService dynamicReportService) {
        this.dynamicReportService = dynamicReportService;
    }

    @PostMapping("/plan")
    @PreAuthorize("hasAuthority('COMPANY_ADMIN')")
    public ResponseEntity<DynamicReportPlanDto> plan(
            @RequestBody DynamicReportRequestDto request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(dynamicReportService.plan(resolveUser(authentication), request));
    }

    @PostMapping("/download")
    @PreAuthorize("hasAuthority('COMPANY_ADMIN')")
    public ResponseEntity<byte[]> download(
            @RequestBody DynamicReportRequestDto request,
            Authentication authentication
    ) {
        GeneratedReportFileDto file = dynamicReportService.generate(resolveUser(authentication), request);
        return ResponseEntity.ok()
                .contentType(file.mediaType())
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(file.fileName()).build().toString()
                )
                .body(file.content());
    }

    private User resolveUser(Authentication authentication) {
        return authentication != null && authentication.getPrincipal() instanceof User
                ? (User) authentication.getPrincipal()
                : null;
    }
}
