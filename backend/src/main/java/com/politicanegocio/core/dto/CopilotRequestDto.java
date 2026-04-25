package com.politicanegocio.core.dto;

import java.util.Map;

public record CopilotRequestDto(
        String userMessage,
        Map<String, Object> currentDiagram
) {
}
