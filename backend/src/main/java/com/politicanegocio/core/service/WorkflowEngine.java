package com.politicanegocio.core.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.politicanegocio.core.model.Policy;
import com.politicanegocio.core.repository.PolicyRepository;
import com.politicanegocio.core.service.WorkflowEngine.WorkflowNode;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class WorkflowEngine {

    private final PolicyRepository policyRepository;
    private final ObjectMapper objectMapper;

    public WorkflowEngine(PolicyRepository policyRepository, ObjectMapper objectMapper) {
        this.policyRepository = policyRepository;
        this.objectMapper = objectMapper;
    }
    
    public List<WorkflowNode> getNextNodes(String policyId, String currentNodeId) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new RuntimeException("Politica no encontrada con ID: " + policyId));

        try {
            JsonNode root = objectMapper.readTree(policy.getDiagramJson());
            Map<String, JsonNode> nodesById = new LinkedHashMap<>();
            Map<String, List<String>> outgoingByNode = new HashMap<>();

            if (root.has("cells")) {
                for (JsonNode cell : root.get("cells")) {
                    if (isWorkflowNode(cell)) {
                        nodesById.put(cell.path("id").asText(), cell);
                    } else if (isLink(cell)) {
                        String sourceId = cell.path("source").path("id").asText();
                        String targetId = cell.path("target").path("id").asText();
                        if (!sourceId.isBlank() && !targetId.isBlank()) {
                            outgoingByNode.computeIfAbsent(sourceId, ignored -> new ArrayList<>()).add(targetId);
                        }
                    }
                }
            }

            String effectiveCurrentNodeId = currentNodeId;
            if ("START".equalsIgnoreCase(currentNodeId)) {
                effectiveCurrentNodeId = findStartNodeId(nodesById);
            }

            List<String> nextNodeIds = outgoingByNode.getOrDefault(effectiveCurrentNodeId, Collections.emptyList());
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
}
