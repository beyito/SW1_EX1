package com.politicanegocio.core.dto;

import java.util.List;

public record IntelligentReceptionResponseDto(
        List<IntelligentReceptionCandidateDto> candidates
) {
}
