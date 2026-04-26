package com.politicanegocio.core.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Document(collection = "copilot_conversations")
public class CopilotConversation {
    @Id
    private String id;
    private String ownerUsername;
    private String company;
    private String policyId;
    private String policyName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<CopilotMessage> messages = new ArrayList<>();

    @Data
    public static class CopilotMessage {
        private String role;
        private String text;
        private LocalDateTime timestamp;
        private List<String> suggestedActions = new ArrayList<>();
    }
}
