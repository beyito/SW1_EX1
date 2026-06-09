package com.politicanegocio.core.dto;

import java.util.List;
import java.util.Map;

public record DynamicReportPlanDto(
        boolean complete,
        List<String> missingFields,
        String question,
        String dataScope,
        Map<String, Object> criteria,
        String format,
        String title,
        String summary,
        List<Map<String, Object>> rows,
        List<String> warnings
) {
}
