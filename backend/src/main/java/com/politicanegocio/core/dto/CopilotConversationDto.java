package com.politicanegocio.core.dto;

import java.time.LocalDateTime;
import java.util.List;

public record CopilotConversationDto(
        String conversationId,
        String policyId,
        String policyName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<CopilotHistoryMessageDto> messages
) {
}
