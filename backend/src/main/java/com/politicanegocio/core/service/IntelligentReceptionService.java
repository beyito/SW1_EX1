package com.politicanegocio.core.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.politicanegocio.core.dto.IntelligentReceptionCandidateDto;
import com.politicanegocio.core.dto.IntelligentReceptionRequestDto;
import com.politicanegocio.core.dto.IntelligentReceptionResponseDto;
import com.politicanegocio.core.model.Policy;
import com.politicanegocio.core.model.PolicyInitialRequirement;
import com.politicanegocio.core.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.beans.factory.annotation.Value;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class IntelligentReceptionService {
    private static final Logger log = LoggerFactory.getLogger(IntelligentReceptionService.class);

    private final RestClient restClient;
    private final PolicyService policyService;
    private final ObjectMapper objectMapper;

    public IntelligentReceptionService(
            @Value("${app.ai-engine.base-url:http://127.0.0.1:8010}") String aiEngineBaseUrl,
            PolicyService policyService,
            ObjectMapper objectMapper
    ) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(15000);
        factory.setReadTimeout(90000);
        this.restClient = RestClient.builder()
                .baseUrl(aiEngineBaseUrl)
                .requestFactory(factory)
                .build();
        this.policyService = policyService;
        this.objectMapper = objectMapper;
    }

    public IntelligentReceptionResponseDto recommendPolicies(IntelligentReceptionRequestDto request, User user) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado.");
        }
        String text = request != null && request.text() != null ? request.text().trim() : "";
        if (text.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Describe que tramite necesitas iniciar.");
        }

        List<Policy> policies = policyService.getStartablePoliciesForUser(user);
        if (policies == null || policies.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No hay tramites disponibles para tu usuario.");
        }

        String requestId = UUID.randomUUID().toString();
        Map<String, Object> payload = new HashMap<>();
        payload.put("text", text);
        payload.put("policies", policies.stream().map(this::toAiPolicy).toList());

        try {
            String rawResponse = restClient.post()
                    .uri("/api/v1/agent/policy-intent")
                    .header("X-Request-Id", requestId)
                    .body(payload)
                    .retrieve()
                    .body(String.class);

            JsonNode result = objectMapper.readTree(rawResponse == null ? "{}" : rawResponse);
            Map<String, Policy> policyById = new HashMap<>();
            for (Policy policy : policies) {
                policyById.put(policy.getId(), policy);
            }

            List<IntelligentReceptionCandidateDto> candidates = parseCandidates(result, policyById);
            if (candidates.isEmpty()) {
                candidates = localRecommendations(text, policies, "Fallback local porque el agente no devolvio candidatos validos.");
            }

            candidates.sort(Comparator.comparingDouble(IntelligentReceptionCandidateDto::confidence).reversed());
            return new IntelligentReceptionResponseDto(candidates);
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (ResourceAccessException exception) {
            log.error("Intelligent reception AI connectivity error requestId={}", requestId, exception);
            return new IntelligentReceptionResponseDto(localRecommendations(text, policies, "Fallback local porque el agente inteligente no esta disponible."));
        } catch (RestClientResponseException exception) {
            log.error("Intelligent reception AI bad response requestId={} status={} body={}", requestId, exception.getStatusCode(), exception.getResponseBodyAsString(), exception);
            return new IntelligentReceptionResponseDto(localRecommendations(text, policies, "Fallback local porque el agente inteligente devolvio una respuesta invalida."));
        } catch (Exception exception) {
            log.error("Intelligent reception unexpected error requestId={}", requestId, exception);
            return new IntelligentReceptionResponseDto(localRecommendations(text, policies, "Fallback local por error inesperado del agente inteligente."));
        }
    }

    private List<IntelligentReceptionCandidateDto> parseCandidates(JsonNode result, Map<String, Policy> policyById) {
        List<IntelligentReceptionCandidateDto> candidates = new ArrayList<>();
        JsonNode candidatesNode = result.path("candidates");
        if (candidatesNode.isArray()) {
            for (JsonNode item : candidatesNode) {
                addCandidate(candidates, item, policyById);
            }
        } else {
            addCandidate(candidates, result, policyById);
        }
        return new ArrayList<>(candidates.stream()
                .filter(candidate -> candidate.policyId() != null && !candidate.policyId().isBlank())
                .distinct()
                .limit(5)
                .toList());
    }

    private void addCandidate(List<IntelligentReceptionCandidateDto> candidates, JsonNode item, Map<String, Policy> policyById) {
        String policyId = item.path("policyId").asText("").trim();
        Policy policy = policyById.get(policyId);
        if (policy == null) {
            return;
        }

        candidates.add(new IntelligentReceptionCandidateDto(
                policy.getId(),
                policy.getName(),
                Math.min(Math.max(item.path("confidence").asDouble(0), 0), 1),
                requiredRequirementNames(policy),
                item.path("reason").asText("")
        ));
    }

    private List<String> requiredRequirementNames(Policy policy) {
        if (policy.getInitialRequirements() == null) {
            return List.of();
        }
        return policy.getInitialRequirements().stream()
                .filter(PolicyInitialRequirement::isRequired)
                .map(PolicyInitialRequirement::getName)
                .filter(name -> name != null && !name.isBlank())
                .toList();
    }

    private Map<String, Object> toAiPolicy(Policy policy) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", policy.getId());
        item.put("name", policy.getName());
        item.put("description", policy.getDescription());
        item.put("initial_requirements", policy.getInitialRequirements() == null
                ? List.of()
                : policy.getInitialRequirements().stream().map(this::toAiRequirement).toList());
        return item;
    }

    private Map<String, Object> toAiRequirement(PolicyInitialRequirement requirement) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", requirement.getId());
        item.put("name", requirement.getName());
        item.put("required", requirement.isRequired());
        return item;
    }

    private List<IntelligentReceptionCandidateDto> localRecommendations(String text, List<Policy> policies, String reason) {
        Set<String> textTokens = tokenize(text);
        return policies.stream()
                .map(policy -> {
                    Set<String> policyTokens = tokenize((policy.getName() == null ? "" : policy.getName()) + " " + (policy.getDescription() == null ? "" : policy.getDescription()));
                    Set<String> union = new HashSet<>(textTokens);
                    union.addAll(policyTokens);
                    Set<String> intersection = new HashSet<>(textTokens);
                    intersection.retainAll(policyTokens);
                    double confidence = union.isEmpty() ? 0.0 : (double) intersection.size() / (double) union.size();
                    if (confidence == 0.0 && policy.getName() != null && !policy.getName().isBlank()) {
                        confidence = text.toLowerCase().contains(policy.getName().toLowerCase()) ? 0.75 : 0.15;
                    }
                    return new IntelligentReceptionCandidateDto(
                            policy.getId(),
                            policy.getName(),
                            Math.min(Math.max(confidence, 0.05), 0.99),
                            requiredRequirementNames(policy),
                            reason
                    );
                })
                .sorted(Comparator.comparingDouble(IntelligentReceptionCandidateDto::confidence).reversed())
                .limit(3)
                .toList();
    }

    private Set<String> tokenize(String value) {
        if (value == null || value.isBlank()) {
            return Set.of();
        }
        String normalized = value.toLowerCase().replaceAll("[^a-z0-9]+", " ");
        return Arrays.stream(normalized.split("\\s+"))
                .map(String::trim)
                .filter(token -> token.length() > 2)
                .collect(java.util.stream.Collectors.toSet());
    }
}
