package com.politicanegocio.core.websocket;

import com.politicanegocio.core.dto.DiagramEventDto;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class DesignerSocketController {

    private final SimpMessagingTemplate messagingTemplate;

    public DesignerSocketController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/policy/{policyId}/change") // El cliente envía a: /app/policy/123/change
public void handlePolicyChange(
        @DestinationVariable String policyId,
        DiagramEventDto event
) {
    // El destino ahora debe empezar por uno de los prefijos del relay
    // RabbitMQ creará automáticamente un topic exchange si usas /topic/
    messagingTemplate.convertAndSend("/topic/policy." + policyId, event);
}
}
