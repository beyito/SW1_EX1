package com.politicanegocio.core.dto;

import java.util.List;
import java.util.Map;

public record CopilotApplyResponseDto(
        String summary,
        String operation,
        List<String> changes,
        List<String> warnings,
        Map<String, Object> diagram,
        List<Map<String, Object>> lanes
) {
}
