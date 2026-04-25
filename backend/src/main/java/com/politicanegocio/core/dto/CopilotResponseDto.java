package com.politicanegocio.core.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.util.List;

public record CopilotResponseDto(
        String message,
        @JsonAlias("suggested_actions") List<String> suggestedActions
) {
}
