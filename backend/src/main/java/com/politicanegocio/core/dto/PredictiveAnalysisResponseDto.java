package com.politicanegocio.core.dto;

import java.util.List;
import java.util.Map;

public record PredictiveAnalysisResponseDto(
        String modelStrategy,
        List<Map<String, Object>> anomalies,
        List<Map<String, Object>> priorities,
        List<Map<String, Object>> routePredictions,
        List<Map<String, Object>> bottlenecks,
        List<String> warnings
) {
}
