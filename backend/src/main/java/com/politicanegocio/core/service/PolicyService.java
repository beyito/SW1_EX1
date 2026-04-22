package com.politicanegocio.core.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.politicanegocio.core.model.Lane;
import com.politicanegocio.core.model.Policy;
import com.politicanegocio.core.model.TaskExecutionOrder;
import com.politicanegocio.core.model.TaskOrder;
import com.politicanegocio.core.model.User;
import com.politicanegocio.core.repository.PolicyRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class PolicyService {

    private final PolicyRepository policyRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PolicyService(PolicyRepository policyRepository) {
        this.policyRepository = policyRepository;
    }

   public Policy createPolicy(String name, String description) {
        
        try {
            User currentUser = getCurrentUser();

            Policy policy = new Policy();
            policy.setName(name);
            policy.setDescription(description);
            policy.setDiagramJson("{\"cells\":[]}");
            policy.setCompanyId(currentUser.getCompany());
            policy.setCompanyName(currentUser.getParentCompany());
            policy.setCreatedBy(currentUser.getUsername());
            policy.setCreatedAt(LocalDateTime.now());
            policy.setUpdatedAt(LocalDateTime.now());

            Policy savedPolicy = policyRepository.save(policy);
            
            return savedPolicy;

        } catch (Exception e) {
            e.printStackTrace();
            throw e; // Relanzamos el error para que el Frontend sepa que falló
        }
    }

    public List<Policy> getAllPolicies() {
        User currentUser = getCurrentUser();
        return policyRepository.findByCompanyId(currentUser.getCompany());
    }

    public List<Policy> getStartablePoliciesForUser(User user) {
        if (user == null) {
            return List.of();
        }
        String userLaneId = resolveUserLaneId(user);
        if (userLaneId.isBlank()) {
            return List.of();
        }

        List<Policy> companyPolicies = policyRepository.findByCompanyId(user.getCompany());
        List<Policy> startablePolicies = new ArrayList<>();

        for (Policy policy : companyPolicies) {
            String startLaneId = extractStartLaneId(policy.getDiagramJson());
            if (userLaneId.equals(startLaneId)) {
                startablePolicies.add(policy);
            }
        }
        return startablePolicies;
    }

    public boolean canUserStartPolicy(User user, Policy policy) {
        if (user == null || policy == null) {
            return false;
        }
        String userLaneId = resolveUserLaneId(user);
        if (userLaneId.isBlank()) {
            return false;
        }
        String startLaneId = extractStartLaneId(policy.getDiagramJson());
        return userLaneId.equals(startLaneId);
    }

    public Policy getPolicyById(String id) {
        Policy policy = policyRepository.findById(id).orElse(null);
        if (policy == null) {
            return null;
        }
        User currentUser = getCurrentUser();
        if (!Objects.equals(policy.getCompanyId(), currentUser.getCompany())) {
            throw new RuntimeException("Acceso denegado a politicas de otra empresa");
        }
        return policy;
    }

    public Policy updatePolicyGraph(String policyId, String diagramJson, List<Lane> lanes) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new RuntimeException("Politica no encontrada con ID: " + policyId));

        User currentUser = getCurrentUser();
        if (!Objects.equals(policy.getCompanyId(), currentUser.getCompany())) {
            throw new RuntimeException("Acceso denegado a politicas de otra empresa");
        }

        try {
            JsonNode rootNode = objectMapper.readTree(diagramJson);

            if (rootNode.has("cells")) {
                for (JsonNode cell : rootNode.get("cells")) {
                    if (isWorkflowNode(cell)) {
                        double nodeX = getCellXPosition(cell);
                        String calculatedLaneId = determineLaneId(nodeX, lanes);
                        ((ObjectNode) cell).put("laneId", calculatedLaneId);
                    }
                }
            }

            String updatedDiagramJson = objectMapper.writeValueAsString(rootNode);
            policy.setDiagramJson(updatedDiagramJson);
            policy.setLanes(lanes == null ? List.of() : lanes);
            policy.setUpdatedAt(LocalDateTime.now());
            return policyRepository.save(policy);
        } catch (Exception exception) {
            throw new RuntimeException("Error al procesar el JSON del diagrama: " + exception.getMessage());
        }
    }

    public TaskExecutionOrder getTaskExecutionOrder(String policyId) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new RuntimeException("Politica no encontrada con ID: " + policyId));

        User currentUser = getCurrentUser();
        if (!Objects.equals(policy.getCompanyId(), currentUser.getCompany())) {
            throw new RuntimeException("Acceso denegado a politicas de otra empresa");
        }

        try {
            JsonNode diagramJson = objectMapper.readTree(policy.getDiagramJson());
            List<TaskOrder> taskOrders = calculateWorkflowExecutionPath(diagramJson, policy.getLanes());

            return new TaskExecutionOrder(policyId, policy.getName(), taskOrders);
        } catch (Exception exception) {
            throw new RuntimeException("Error al calcular el orden de tareas: " + exception.getMessage());
        }
    }

    private User getCurrentUser() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !(auth.getPrincipal() instanceof User user)) {
        throw new RuntimeException("Usuario no autenticado");
    }
    return user;
}

    private List<TaskOrder> calculateWorkflowExecutionPath(JsonNode diagramJson, List<Lane> lanes) {
        Map<String, JsonNode> nodesMap = new HashMap<>();
        Map<String, List<String>> outgoingLinks = new HashMap<>();
        Map<String, List<String>> incomingLinks = new HashMap<>();
        String startNodeId = null;

        if (diagramJson.has("cells")) {
            for (JsonNode cell : diagramJson.get("cells")) {
                String cellType = cell.get("type").asText();
                String cellId = cell.get("id").asText();

                if (isWorkflowNode(cell)) {
                    nodesMap.put(cellId, cell);
                    outgoingLinks.putIfAbsent(cellId, new ArrayList<>());
                    incomingLinks.putIfAbsent(cellId, new ArrayList<>());

                    if (isStartNode(cell)) {
                        startNodeId = cellId;
                    }
                } else if ("standard.Link".equals(cellType)) {
                    if (cell.has("source") && cell.has("target") &&
                        cell.get("source").has("id") && cell.get("target").has("id")) {
                        String sourceId = cell.get("source").get("id").asText();
                        String targetId = cell.get("target").get("id").asText();

                        outgoingLinks.putIfAbsent(sourceId, new ArrayList<>());
                        outgoingLinks.get(sourceId).add(targetId);
                        incomingLinks.putIfAbsent(targetId, new ArrayList<>());
                        incomingLinks.get(targetId).add(sourceId);
                    }
                }
            }
        }

        if (startNodeId == null) {
            throw new RuntimeException("Flujo inválido: No se encontró ningún nodo de tipo 'START'.");
        }

        List<TaskOrder> executionPath = new ArrayList<>();
        Set<String> visited = new LinkedHashSet<>();
        Queue<String> queue = new LinkedList<>();
        queue.add(startNodeId);
        int order = 1;

        while (!queue.isEmpty()) {
            String currentNodeId = queue.poll();
            if (visited.contains(currentNodeId)) {
                continue;
            }

            visited.add(currentNodeId);
            JsonNode nodeData = nodesMap.get(currentNodeId);

            if (nodeData != null) {
                String nodeLabel = getNodeLabel(nodeData);
                String nodeType = getNodeType(nodeData);
                String laneId = getLaneId(nodeData);
                String laneName = getLaneName(laneId, lanes);
                List<String> dependencies = incomingLinks.getOrDefault(currentNodeId, new ArrayList<>());

                TaskOrder taskOrder = new TaskOrder(currentNodeId, nodeLabel, nodeType, order++, dependencies, laneId, laneName);
                executionPath.add(taskOrder);

                List<String> nextNodes = outgoingLinks.getOrDefault(currentNodeId, new ArrayList<>());
                queue.addAll(nextNodes);
            }
        }

        return executionPath;
    }

    private double getCellXPosition(JsonNode cell) {
    if (cell.has("position") && cell.get("position").has("x")) {
        // 🚩 Ahora sí: entramos a "position" y luego extraemos la "x"
        return cell.get("position").get("x").asDouble(); 
    }
    return 0.0;
}

    private String determineLaneId(double xPosition, List<Lane> lanes) {
        if (lanes == null || lanes.isEmpty()) {
            return "default";
        }
        double laneWidth = 1200.0 / lanes.size();
        double nodeCenterX = xPosition + 70.0;
        int laneIndex = Math.max(0, Math.min(lanes.size() - 1, (int) (nodeCenterX / laneWidth)));
        return lanes.get(laneIndex).getId();
    }

    private boolean isWorkflowNode(JsonNode node) {
        if (!node.has("type")) return false;
        String type = node.get("type").asText();
        return ("standard.Rectangle".equals(type) || "standard.Circle".equals(type) || "standard.Polygon".equals(type))
                && node.has("nodeType");
    }

    private boolean isStartNode(JsonNode node) {
        return isWorkflowNode(node) && "START".equals(node.get("nodeType").asText());
    }

    private String getNodeLabel(JsonNode node) {
        if (node.has("attrs") && node.get("attrs").has("label") && node.get("attrs").get("label").has("text")) {
            return node.get("attrs").get("label").get("text").asText();
        }
        return "Sin etiqueta";
    }

    private String getNodeType(JsonNode node) {
        return node.has("nodeType") ? node.get("nodeType").asText() : "UNKNOWN";
    }

    private String getLaneId(JsonNode node) {
        return node.has("laneId") ? node.get("laneId").asText() : "default";
    }

    private String resolveUserLaneId(User user) {
        if (user == null) {
            return "";
        }
        if (user.getLaneId() != null && !user.getLaneId().isBlank()) {
            return user.getLaneId().trim();
        }
        return user.getArea() != null ? user.getArea().trim() : "";
    }

    private String extractStartLaneId(String diagramJson) {
        if (diagramJson == null || diagramJson.isBlank()) {
            return "";
        }
        try {
            JsonNode rootNode = objectMapper.readTree(diagramJson);
            JsonNode cellsNode = rootNode.path("cells");
            if (!cellsNode.isArray()) {
                return "";
            }
            for (JsonNode cell : cellsNode) {
                if (!isWorkflowNode(cell)) {
                    continue;
                }
                if ("START".equals(cell.path("nodeType").asText())) {
                    return cell.path("laneId").asText("");
                }
            }
            return "";
        } catch (Exception exception) {
            return "";
        }
    }

    private String getLaneName(String laneId, List<Lane> lanes) {
        if (lanes == null || lanes.isEmpty() || "default".equals(laneId)) {
            return "Sin carril";
        }
        return lanes.stream()
                .filter(lane -> lane.getId().equals(laneId))
                .map(Lane::getName)
                .findFirst()
                .orElse("Sin carril");
    }
}
