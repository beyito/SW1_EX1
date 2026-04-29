package com.politicanegocio.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.politicanegocio.core.dto.CopilotApplyRequestDto;
import com.politicanegocio.core.dto.CopilotApplyResponseDto;
import com.politicanegocio.core.dto.CopilotConversationDto;
import com.politicanegocio.core.dto.CopilotHistoryMessageDto;
import com.politicanegocio.core.dto.CopilotRequestDto;
import com.politicanegocio.core.dto.CopilotResponseDto;
import com.politicanegocio.core.dto.VoiceFillRequestDto;
import com.politicanegocio.core.model.CopilotConversation;
import com.politicanegocio.core.model.User;
import com.politicanegocio.core.repository.CopilotConversationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.HashSet;
import java.util.UUID;

@Service
public class CopilotService {
    private static final Logger log = LoggerFactory.getLogger(CopilotService.class);
    private static final int MAX_HISTORY_FOR_AI = 20;

    private final RestClient restClient;
    private final String aiEngineBaseUrl;
    private final CopilotConversationRepository copilotConversationRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CopilotService(
            @Value("${app.ai-engine.base-url:http://127.0.0.1:8010}") String aiEngineBaseUrl,
            CopilotConversationRepository copilotConversationRepository
    ) {
        this.aiEngineBaseUrl = aiEngineBaseUrl;
        this.copilotConversationRepository = copilotConversationRepository;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(20000);
        factory.setReadTimeout(300000);

        this.restClient = RestClient.builder()
                .baseUrl(aiEngineBaseUrl)
                .requestFactory(factory)
                .build();

        log.info("CopilotService initialized with aiEngineBaseUrl={}", aiEngineBaseUrl);
    }

    public CopilotResponseDto chat(CopilotRequestDto request, User actor) {
        String requestId = UUID.randomUUID().toString();
        User safeActor = requireActor(actor);

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request vacio para copilot.");
        }
        String userMessage = request.userMessage() == null ? "" : request.userMessage().trim();
        if (userMessage.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userMessage es obligatorio.");
        }

        CopilotConversation conversation = resolveConversation(safeActor, request);
        List<Map<String, Object>> historyForAi = toAiHistory(conversation.getMessages());

        long startMillis = System.currentTimeMillis();
        log.info(
                "Copilot gateway request start requestId={} aiBaseUrl={} messageLength={} hasDiagram={} historySize={} conversationId={}",
                requestId,
                aiEngineBaseUrl,
                userMessage.length(),
                request.currentDiagram() != null,
                historyForAi.size(),
                conversation.getId()
        );

        try {
            Map<String, Object> aiPayload = new HashMap<>();
            aiPayload.put("userMessage", userMessage);
            aiPayload.put("currentDiagram", request.currentDiagram() == null ? Map.of() : request.currentDiagram());
            aiPayload.put("history", historyForAi);
            
            // ---> NUEVO: PASAMOS LOS CARRILES Y EL CONTEXTO A PYTHON <---
            aiPayload.put("lanes", request.lanes() == null ? List.of() : request.lanes());
            if (request.context() != null && !request.context().isBlank()) {
                aiPayload.put("context", request.context()); 
            }
            // -------------------------------------------------------------

            String aiRawResponse = restClient.post()
                    .uri("/api/ai/copilot-chat")
                    .header("X-Request-Id", requestId)
                    .body(aiPayload)
                    .retrieve()
                    .body(String.class);

            CopilotResponseDto aiResponse = parseChatResponse(aiRawResponse, requestId);

            if (aiResponse == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "El microservicio IA devolvio respuesta vacia. requestId=" + requestId
                );
            }

            appendMessage(conversation, "user", userMessage, List.of());
            appendMessage(conversation, "assistant", aiResponse.message(), aiResponse.suggestedActions());
            conversation.setUpdatedAt(LocalDateTime.now());
            if (request.policyName() != null && !request.policyName().isBlank()) {
                conversation.setPolicyName(request.policyName().trim());
            }
            CopilotConversation savedConversation = copilotConversationRepository.save(conversation);

            log.info(
                    "Copilot gateway request success requestId={} elapsedMs={} suggestedActions={} conversationId={}",
                    requestId,
                    (System.currentTimeMillis() - startMillis),
                    aiResponse.suggestedActions() == null ? 0 : aiResponse.suggestedActions().size(),
                    savedConversation.getId()
            );

            return new CopilotResponseDto(
                    aiResponse.message(),
                    aiResponse.suggestedActions(),
                    savedConversation.getId()
            );
        } catch (ResourceAccessException exception) {
            String rootCause = exception.getMostSpecificCause() != null
                    ? exception.getMostSpecificCause().toString()
                    : exception.toString();
            boolean timedOut = rootCause.toLowerCase().contains("timed out");

            log.error(
                    "Copilot gateway connectivity error requestId={} aiBaseUrl={} rootCause={}",
                    requestId,
                    aiEngineBaseUrl,
                    rootCause,
                    exception
            );

            throw new ResponseStatusException(
                    timedOut ? HttpStatus.GATEWAY_TIMEOUT : HttpStatus.BAD_GATEWAY,
                    (timedOut ? "Timeout esperando respuesta del microservicio IA. " : "No se pudo conectar con el microservicio IA. ")
                            + "requestId=" + requestId
                            + " aiBaseUrl=" + aiEngineBaseUrl
                            + " rootCause=" + rootCause,
                    exception
            );
        } catch (RestClientResponseException exception) {
            String body = exception.getResponseBodyAsString();
            String trimmedBody = body == null ? "" : body.trim();
            if (trimmedBody.length() > 1000) {
                trimmedBody = trimmedBody.substring(0, 1000) + "...";
            }

            log.error(
                    "Copilot gateway ai response error requestId={} status={} body={}",
                    requestId,
                    exception.getStatusCode(),
                    trimmedBody,
                    exception
            );

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Error del microservicio IA. requestId=" + requestId
                            + " status=" + exception.getStatusCode().value()
                            + " body=" + trimmedBody,
                    exception
            );
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            log.error(
                    "Copilot gateway unexpected error requestId={} aiBaseUrl={}",
                    requestId,
                    aiEngineBaseUrl,
                    exception
            );

            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Fallo interno en gateway de copilot. requestId=" + requestId
                            + " message=" + exception.getMessage(),
                    exception
            );
        }
    }

    public CopilotConversationDto getConversationHistory(User actor, String policyId, String conversationId) {
        User safeActor = requireActor(actor);

        CopilotConversation conversation = null;
        if (conversationId != null && !conversationId.isBlank()) {
            conversation = copilotConversationRepository.findByIdAndOwnerUsername(conversationId.trim(), safeActor.getUsername())
                    .orElse(null);
        } else if (policyId != null && !policyId.isBlank()) {
            conversation = copilotConversationRepository
                    .findTopByOwnerUsernameAndPolicyIdOrderByUpdatedAtDesc(safeActor.getUsername(), policyId.trim())
                    .orElse(null);
        }

        if (conversation == null) {
            return new CopilotConversationDto(
                    null,
                    policyId,
                    null,
                    null,
                    null,
                    List.of()
            );
        }

        return toConversationDto(conversation);
    }

    public Map<String, Object> fillFormFromVoice(VoiceFillRequestDto request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request vacio para voice-fill.");
        }
        String transcript = request.voiceTranscript() == null ? "" : request.voiceTranscript().trim();
        if (transcript.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "voiceTranscript es obligatorio.");
        }
        if (request.formFields() == null || request.formFields().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "formFields es obligatorio.");
        }

        String requestId = UUID.randomUUID().toString();
        Map<String, Object> payload = new HashMap<>();
        payload.put("voice_transcript", transcript);
        payload.put("form_fields", request.formFields());

        try {
            String aiRawResponse = restClient.post()
                    .uri("/api/v1/agent/voice-fill")
                    .header("X-Request-Id", requestId)
                    .body(payload)
                    .retrieve()
                    .body(String.class);

            if (aiRawResponse == null || aiRawResponse.trim().isEmpty()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "El microservicio IA devolvio respuesta vacia (voice-fill). requestId=" + requestId
                );
            }

            Map<String, Object> parsed = objectMapper.readValue(aiRawResponse, Map.class);
            Set<String> allowed = new HashSet<>(request.formFields());
            parsed.keySet().removeIf(key -> !allowed.contains(key));
            return parsed;
        } catch (ResourceAccessException exception) {
            String rootCause = exception.getMostSpecificCause() != null
                    ? exception.getMostSpecificCause().toString()
                    : exception.toString();
            boolean timedOut = rootCause.toLowerCase().contains("timed out");
            throw new ResponseStatusException(
                    timedOut ? HttpStatus.GATEWAY_TIMEOUT : HttpStatus.BAD_GATEWAY,
                    (timedOut ? "Timeout esperando respuesta del microservicio IA (voice-fill). " : "No se pudo conectar con el microservicio IA (voice-fill). ")
                            + "requestId=" + requestId
                            + " rootCause=" + rootCause,
                    exception
            );
        } catch (RestClientResponseException exception) {
            String body = exception.getResponseBodyAsString();
            String trimmedBody = body == null ? "" : body.trim();
            if (trimmedBody.length() > 1000) {
                trimmedBody = trimmedBody.substring(0, 1000) + "...";
            }
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Error del microservicio IA (voice-fill). requestId=" + requestId
                            + " status=" + exception.getStatusCode().value()
                            + " body=" + trimmedBody,
                    exception
            );
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Fallo interno en voice-fill. requestId=" + requestId + " message=" + exception.getMessage(),
                    exception
            );
        }
    }

   public CopilotApplyResponseDto apply(CopilotApplyRequestDto request) {
        String requestId = UUID.randomUUID().toString();
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request vacio para apply.");
        }
        String instruction = request.instruction() == null ? "" : request.instruction().trim();
        if (instruction.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "instruction es obligatoria.");
        }
        if (request.currentDiagram() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "currentDiagram es obligatorio.");
        }

        // ---> CORRECCIÓN ARQUITECTÓNICA: Usamos HashMap para evitar NullPointerExceptions <---
        Map<String, Object> payload = new HashMap<>();
        payload.put("operation", "modify");
        payload.put("instruction", instruction);
        payload.put("current_diagram", request.currentDiagram());
        payload.put("lanes", request.lanes() == null ? java.util.List.of() : request.lanes());
        
        // Protegemos el context asumiendo que es un String como en el chat
        if (request.context() != null && !request.context().toString().isBlank()) {
            payload.put("context", request.context());
        }
        // ----------------------------------------------------------------------------------

        try {
            String aiRawResponse = restClient.post()
                    .uri("/api/v1/agent/diagram") // <-- IMPORTANTE: Verifica que esta sea la URL correcta de tu Python
                    .header("X-Request-Id", requestId)
                    .body(payload)
                    .retrieve()
                    .body(String.class);

            CopilotApplyResponseDto response = parseApplyResponse(aiRawResponse, requestId);

            if (response == null || response.diagram() == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "El microservicio IA no devolvio diagrama aplicable. requestId=" + requestId
                );
            }
            return response;
        } catch (ResourceAccessException exception) {
            String rootCause = exception.getMostSpecificCause() != null
                    ? exception.getMostSpecificCause().toString()
                    : exception.toString();
            boolean timedOut = rootCause.toLowerCase().contains("timed out");
            throw new ResponseStatusException(
                    timedOut ? HttpStatus.GATEWAY_TIMEOUT : HttpStatus.BAD_GATEWAY,
                    (timedOut ? "Timeout esperando respuesta del microservicio IA (apply). " : "No se pudo conectar con el microservicio IA (apply). ")
                            + "requestId=" + requestId
                            + " rootCause=" + rootCause,
                    exception
            );
        } catch (RestClientResponseException exception) {
            String body = exception.getResponseBodyAsString();
            String trimmedBody = body == null ? "" : body.trim();
            if (trimmedBody.length() > 1000) {
                trimmedBody = trimmedBody.substring(0, 1000) + "...";
            }
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Error del microservicio IA (apply). requestId=" + requestId
                            + " status=" + exception.getStatusCode().value()
                            + " body=" + trimmedBody,
                    exception
            );
        }
    }

    private User requireActor(User actor) {
        if (actor == null || actor.getUsername() == null || actor.getUsername().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado para Copilot.");
        }
        return actor;
    }

    private CopilotConversation resolveConversation(User actor, CopilotRequestDto request) {
        String conversationId = request.conversationId() == null ? "" : request.conversationId().trim();
        String policyId = request.policyId() == null ? "" : request.policyId().trim();
        String policyName = request.policyName() == null ? "" : request.policyName().trim();

        CopilotConversation conversation = null;
        if (!conversationId.isBlank()) {
            conversation = copilotConversationRepository.findByIdAndOwnerUsername(conversationId, actor.getUsername())
                    .orElse(null);
        }

        if (conversation == null && !policyId.isBlank()) {
            conversation = copilotConversationRepository
                    .findTopByOwnerUsernameAndPolicyIdOrderByUpdatedAtDesc(actor.getUsername(), policyId)
                    .orElse(null);
        }

        if (conversation != null) {
            if (!policyId.isBlank()) {
                conversation.setPolicyId(policyId);
            }
            if (!policyName.isBlank()) {
                conversation.setPolicyName(policyName);
            }
            return conversation;
        }

        CopilotConversation newConversation = new CopilotConversation();
        newConversation.setOwnerUsername(actor.getUsername());
        newConversation.setCompany(actor.getCompany());
        newConversation.setPolicyId(policyId.isBlank() ? null : policyId);
        newConversation.setPolicyName(policyName.isBlank() ? null : policyName);
        LocalDateTime now = LocalDateTime.now();
        newConversation.setCreatedAt(now);
        newConversation.setUpdatedAt(now);
        return newConversation;
    }

    private List<Map<String, Object>> toAiHistory(List<CopilotConversation.CopilotMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }
        int fromIndex = Math.max(0, messages.size() - MAX_HISTORY_FOR_AI);
        List<CopilotConversation.CopilotMessage> recent = messages.subList(fromIndex, messages.size());

        List<Map<String, Object>> history = new ArrayList<>(recent.size());
        for (CopilotConversation.CopilotMessage message : recent) {
            if (message == null || message.getText() == null || message.getText().isBlank()) {
                continue;
            }
            Map<String, Object> item = new HashMap<>();
            item.put("role", message.getRole() == null ? "assistant" : message.getRole());
            item.put("text", message.getText());
            if (message.getTimestamp() != null) {
                item.put("timestamp", message.getTimestamp().toString());
            }
            history.add(item);
        }
        return history;
    }

    private void appendMessage(
            CopilotConversation conversation,
            String role,
            String text,
            List<String> suggestedActions
    ) {
        if (text == null || text.isBlank()) {
            return;
        }
        CopilotConversation.CopilotMessage message = new CopilotConversation.CopilotMessage();
        message.setRole(role);
        message.setText(text);
        message.setTimestamp(LocalDateTime.now());
        message.setSuggestedActions(suggestedActions == null ? List.of() : suggestedActions.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList());
        conversation.getMessages().add(message);
    }

    private CopilotConversationDto toConversationDto(CopilotConversation conversation) {
        List<CopilotHistoryMessageDto> messages = conversation.getMessages() == null
                ? List.of()
                : conversation.getMessages().stream()
                .map(message -> new CopilotHistoryMessageDto(
                        message.getRole(),
                        message.getText(),
                        message.getTimestamp(),
                        message.getSuggestedActions() == null ? List.of() : message.getSuggestedActions()
                ))
                .toList();

        return new CopilotConversationDto(
                conversation.getId(),
                conversation.getPolicyId(),
                conversation.getPolicyName(),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt(),
                messages
        );
    }

    private CopilotResponseDto parseChatResponse(String rawBody, String requestId) {
        String payload = rawBody == null ? "" : rawBody.trim();
        if (payload.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "El microservicio IA devolvio respuesta vacia. requestId=" + requestId
            );
        }
        try {
            return objectMapper.readValue(payload, CopilotResponseDto.class);
        } catch (Exception exception) {
            String preview = payload.length() > 1000 ? payload.substring(0, 1000) + "..." : payload;
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Respuesta invalida del microservicio IA (chat). requestId=" + requestId + " body=" + preview,
                    exception
            );
        }
    }

    private CopilotApplyResponseDto parseApplyResponse(String rawBody, String requestId) {
        String payload = rawBody == null ? "" : rawBody.trim();
        if (payload.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "El microservicio IA no devolvio respuesta (apply). requestId=" + requestId
            );
        }
        try {
            return objectMapper.readValue(payload, CopilotApplyResponseDto.class);
        } catch (Exception exception) {
            String preview = payload.length() > 1000 ? payload.substring(0, 1000) + "..." : payload;
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Respuesta invalida del microservicio IA (apply). requestId=" + requestId + " body=" + preview,
                    exception
            );
        }
    }
}
