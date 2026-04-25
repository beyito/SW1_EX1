package com.politicanegocio.core.service;

import com.politicanegocio.core.dto.CopilotRequestDto;
import com.politicanegocio.core.dto.CopilotApplyRequestDto;
import com.politicanegocio.core.dto.CopilotApplyResponseDto;
import com.politicanegocio.core.dto.CopilotResponseDto;
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

import java.util.UUID;
import java.util.Map;

@Service
public class CopilotService {
    private static final Logger log = LoggerFactory.getLogger(CopilotService.class);

    private final RestClient restClient;
    private final String aiEngineBaseUrl;

    public CopilotService(@Value("${app.ai-engine.base-url:http://127.0.0.1:8010}") String aiEngineBaseUrl) {
        this.aiEngineBaseUrl = aiEngineBaseUrl;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(60000);

        this.restClient = RestClient.builder()
                .baseUrl(aiEngineBaseUrl)
                .requestFactory(factory)
                .build();

        log.info("CopilotService initialized with aiEngineBaseUrl={}", aiEngineBaseUrl);
    }

    public CopilotResponseDto chat(CopilotRequestDto request) {
        String requestId = UUID.randomUUID().toString();

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request vacio para copilot.");
        }
        String userMessage = request.userMessage() == null ? "" : request.userMessage().trim();
        if (userMessage.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userMessage es obligatorio.");
        }

        long startMillis = System.currentTimeMillis();
        log.info(
                "Copilot gateway request start requestId={} aiBaseUrl={} messageLength={} hasDiagram={}",
                requestId,
                aiEngineBaseUrl,
                userMessage.length(),
                request.currentDiagram() != null
        );

        try {
            CopilotResponseDto response = restClient.post()
                    .uri("/api/ai/copilot-chat")
                    .header("X-Request-Id", requestId)
                    .body(request)
                    .retrieve()
                    .body(CopilotResponseDto.class);

            if (response == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "El microservicio IA devolvio respuesta vacia. requestId=" + requestId
                );
            }

            log.info(
                    "Copilot gateway request success requestId={} elapsedMs={} suggestedActions={}",
                    requestId,
                    (System.currentTimeMillis() - startMillis),
                    response.suggestedActions() == null ? 0 : response.suggestedActions().size()
            );

            return response;
        } catch (ResourceAccessException exception) {
            String rootCause = exception.getMostSpecificCause() != null
                    ? exception.getMostSpecificCause().toString()
                    : exception.toString();

            log.error(
                    "Copilot gateway connectivity error requestId={} aiBaseUrl={} rootCause={}",
                    requestId,
                    aiEngineBaseUrl,
                    rootCause,
                    exception
            );

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "No se pudo conectar con el microservicio IA. requestId=" + requestId
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

        Map<String, Object> payload = Map.of(
                "operation", "modify",
                "instruction", instruction,
                "current_diagram", request.currentDiagram(),
                "lanes", request.lanes() == null ? java.util.List.of() : request.lanes(),
                "context", request.context() == null ? Map.of() : request.context()
        );

        try {
            CopilotApplyResponseDto response = restClient.post()
                    .uri("/api/v1/agent/diagram")
                    .header("X-Request-Id", requestId)
                    .body(payload)
                    .retrieve()
                    .body(CopilotApplyResponseDto.class);

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
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "No se pudo conectar con el microservicio IA (apply). requestId=" + requestId
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
}
