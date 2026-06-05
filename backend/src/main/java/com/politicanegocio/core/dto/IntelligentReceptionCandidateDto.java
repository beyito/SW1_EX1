package com.politicanegocio.core.dto;

import java.util.List;

public record IntelligentReceptionCandidateDto(
        String policyId,
        String policyName,
        double confidence,
        List<String> missingRequirements,
        String reason
) {
}
