package com.politicanegocio.core.dto;

import org.springframework.http.MediaType;

public record GeneratedReportFileDto(
        String fileName,
        MediaType mediaType,
        byte[] content
) {
}
