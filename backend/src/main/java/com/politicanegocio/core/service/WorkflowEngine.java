package com.politicanegocio.core.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.politicanegocio.core.model.Policy;
import com.politicanegocio.core.repository.PolicyRepository;

import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class WorkflowEngine {
    private static final String NODE_TYPE_DECISION = "DECISION";
    private static final String CONDITION_TYPE_DEFAULT = "default";
    private static final String CONDITION_TYPE_EXPRESSION = "expression";

    private final PolicyRepository policyRepository;
    private final ObjectMapper objectMapper;
    private final ExpressionParser expressionParser = new SpelExpressionParser();

    public WorkflowEngine(PolicyRepository policyRepository, ObjectMapper objectMapper) {
        this.policyRepository = policyRepository;
        this.objectMapper = objectMapper;
    }
    
    public List<WorkflowNode> getNextNodes(String policyId, String currentNodeId) {
        return getNextNodes(policyId, currentNodeId, Map.of());
    }

    public List<WorkflowNode> getNextNodes(String policyId, String currentNodeId, Map<String, Object> routingVariables) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new RuntimeException("Politica no encontrada con ID: " + policyId));

        try {
            JsonNode root = objectMapper.readTree(policy.getDiagramJson());
            Map<String, JsonNode> nodesById = new LinkedHashMap<>();
            Map<String, List<LinkDefinition>> outgoingByNode = new HashMap<>();

            if (root.has("cells")) {
                for (JsonNode cell : root.get("cells")) {
                    if (isWorkflowNode(cell)) {
                        nodesById.put(cell.path("id").asText(), cell);
                    } else if (isLink(cell)) {
                        String sourceId = cell.path("source").path("id").asText();
                        String targetId = cell.path("target").path("id").asText();
                        if (!sourceId.isBlank() && !targetId.isBlank()) {
                            outgoingByNode.computeIfAbsent(sourceId, ignored -> new ArrayList<>())
                                    .add(new LinkDefinition(
                                            targetId,
                                            cell.path("condition").path("type").asText(""),
                                            cell.path("condition").path("script").asText("")
                                    ));
                        }
                    }
                }
            }

            String effectiveCurrentNodeId = currentNodeId;
            if ("START".equalsIgnoreCase(currentNodeId)) {
                effectiveCurrentNodeId = findStartNodeId(nodesById);
            }

            String currentNodeType = nodesById.getOrDefault(effectiveCurrentNodeId, objectMapper.createObjectNode())
                    .path("nodeType")
                    .asText("");

            List<LinkDefinition> outgoingLinks = outgoingByNode.getOrDefault(effectiveCurrentNodeId, Collections.emptyList());
            List<String> nextNodeIds;
            if (NODE_TYPE_DECISION.equalsIgnoreCase(currentNodeType)) {
                nextNodeIds = resolveDecisionTargets(outgoingLinks, routingVariables);
            } else {
                nextNodeIds = outgoingLinks.stream().map(LinkDefinition::targetId).toList();
            }

            List<WorkflowNode> result = new ArrayList<>();
            for (String nextNodeId : nextNodeIds) {
                JsonNode node = nodesById.get(nextNodeId);
                if (node == null) {
                    continue;
                }
                result.add(new WorkflowNode(
                        nextNodeId,
                        node.path("nodeType").asText("UNKNOWN"),
                        getNodeLabel(node),
                        resolveLaneReference(policy, node.path("laneId").asText("default"))
                ));
            }
            return result;
        } catch (Exception exception) {
            throw new RuntimeException("No se pudo resolver el flujo de trabajo: " + exception.getMessage());
        }
    }

    private List<String> resolveDecisionTargets(List<LinkDefinition> outgoingLinks, Map<String, Object> routingVariables) {
        if (outgoingLinks == null || outgoingLinks.isEmpty()) {
            return List.of();
        }

        StandardEvaluationContext context = new StandardEvaluationContext();
        if (routingVariables != null) {
            routingVariables.forEach(context::setVariable);
        }

        List<String> matchedExpressionTargets = outgoingLinks.stream()
                .filter(link -> CONDITION_TYPE_EXPRESSION.equalsIgnoreCase(link.conditionType()))
                .filter(link -> evaluateExpression(link.script(), context))
                .map(LinkDefinition::targetId)
                .filter(Objects::nonNull)
                .toList();
        if (!matchedExpressionTargets.isEmpty()) {
            return matchedExpressionTargets;
        }

        return outgoingLinks.stream()
                .filter(link -> CONDITION_TYPE_DEFAULT.equalsIgnoreCase(link.conditionType()))
                .map(LinkDefinition::targetId)
                .findFirst()
                .map(List::of)
                .orElse(List.of());
    }

    private boolean evaluateExpression(String script, StandardEvaluationContext context) {
        if (script == null || script.isBlank()) {
            return false;
        }
        try {
            Boolean result = expressionParser.parseExpression(script).getValue(context, Boolean.class);
            return Boolean.TRUE.equals(result);
        } catch (Exception ignored) {
            return false;
        }
    }

    public String getNodeName(String policyId, String nodeId) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new RuntimeException("Politica no encontrada con ID: " + policyId));
        try {
            JsonNode root = objectMapper.readTree(policy.getDiagramJson());
            if (!root.has("cells")) {
                return nodeId;
            }

            for (JsonNode cell : root.get("cells")) {
                if (nodeId.equals(cell.path("id").asText()) && isWorkflowNode(cell)) {
                    return getNodeLabel(cell);
                }
            }
            return nodeId;
        } catch (Exception exception) {
            return nodeId;
        }
    }

    private boolean isWorkflowNode(JsonNode node) {
        if (!node.has("type")) {
            return false;
        }
        String type = node.path("type").asText();
        return ("standard.Rectangle".equals(type) || "standard.Circle".equals(type) || "standard.Polygon".equals(type))
                && node.has("nodeType");
    }

    private boolean isLink(JsonNode node) {
        return "standard.Link".equals(node.path("type").asText())
                && node.has("source")
                && node.has("target");
    }

    private String findStartNodeId(Map<String, JsonNode> nodesById) {
        return nodesById.values().stream()
                .filter(node -> "START".equalsIgnoreCase(node.path("nodeType").asText()))
                .map(node -> node.path("id").asText())
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No se encontro un nodo START en la politica"));
    }

    private String getNodeLabel(JsonNode node) {
        return node.path("attrs").path("label").path("text").asText("Sin etiqueta");
    }

    private String resolveLaneReference(Policy policy, String rawLaneId) {
        if (policy.getLanes() == null || policy.getLanes().isEmpty()) {
            return rawLaneId;
        }
        return policy.getLanes().stream()
                .filter(lane -> rawLaneId.equals(lane.getId()))
                .map(lane -> lane.getName() != null ? lane.getName().trim() : rawLaneId)
                .findFirst()
                .orElse(rawLaneId);
    }

    public String getFormSchemaForNode(String policyId, String nodeId) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new RuntimeException("Politica no encontrada con ID: " + policyId));

        try {
            JsonNode root = objectMapper.readTree(policy.getDiagramJson());
            
            if (root.has("cells")) {
                for (JsonNode cell : root.get("cells")) {
                    // Buscamos la celda exacta por su ID
                    if (nodeId.equals(cell.path("id").asText())) {
                        // Verificamos si tiene preguntas configuradas
                        if (cell.has("formSchema")) {
                            JsonNode schemaNode = cell.get("formSchema");
                            // Si está como texto, devolvemos el texto; si es un arreglo, lo convertimos a texto
                            return schemaNode.isTextual() ? schemaNode.asText() : schemaNode.toString();
                        }
                        // Si el nodo no tiene la propiedad formSchema, devolvemos un arreglo vacío
                        return "[]";
                    }
                }
            }
            return "[]";
        } catch (Exception exception) {
            throw new RuntimeException("Error al extraer el esquema del nodo: " + exception.getMessage());
        }
    }
    public List<String> getIncomingNodeIds(String policyId, String targetNodeId) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new RuntimeException("Politica no encontrada con ID: " + policyId));
        
        List<String> incomingIds = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(policy.getDiagramJson());
            if (root.has("cells")) {
                for (JsonNode cell : root.get("cells")) {
                    if (isLink(cell)) {
                        String target = cell.path("target").path("id").asText();
                        if (targetNodeId.equals(target)) {
                            incomingIds.add(cell.path("source").path("id").asText());
                        }
                    }
                }
            }
            return incomingIds;
        } catch (Exception exception) {
            throw new RuntimeException("Error al buscar nodos entrantes: " + exception.getMessage());
        }
    }
    public record WorkflowNode(String nodeId, String nodeType, String nodeLabel, String laneId) {
    }

    private record LinkDefinition(String targetId, String conditionType, String script) {
    }
}
