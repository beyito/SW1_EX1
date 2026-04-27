package com.politicanegocio.core.dto;

import java.util.List;
import java.util.Map;

public record CopilotRequestDto(
        String userMessage,
        Map<String, Object> currentDiagram,
        List<Map<String, Object>> lanes,
        String context,
        String conversationId,
        String policyId,
        String policyName
) {
}
