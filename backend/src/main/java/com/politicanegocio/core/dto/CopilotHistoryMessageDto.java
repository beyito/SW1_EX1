package com.politicanegocio.core.dto;

import java.time.LocalDateTime;
import java.util.List;

public record CopilotHistoryMessageDto(
        String role,
        String text,
        LocalDateTime timestamp,
        List<String> suggestedActions
) {
}
