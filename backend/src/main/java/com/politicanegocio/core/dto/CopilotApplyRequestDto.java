package com.politicanegocio.core.dto;

import java.util.List;
import java.util.Map;

public record CopilotApplyRequestDto(
        String instruction,
        Map<String, Object> currentDiagram,
        List<Map<String, Object>> lanes,
        Map<String, Object> context
) {
}
