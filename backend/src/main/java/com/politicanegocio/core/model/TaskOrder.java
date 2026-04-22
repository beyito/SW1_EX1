package com.politicanegocio.core.model;

import java.util.List;

public class TaskOrder {
    private String nodeId;
    private String nodeLabel;
    private String nodeType;
    private int order;
    private List<String> dependencies;
    private String laneId;
    private String laneName;

    public TaskOrder() {}

    public TaskOrder(String nodeId, String nodeLabel, String nodeType, int order, List<String> dependencies) {
        this.nodeId = nodeId;
        this.nodeLabel = nodeLabel;
        this.nodeType = nodeType;
        this.order = order;
        this.dependencies = dependencies;
        
    }

    public TaskOrder(String nodeId, String nodeLabel, String nodeType, int order, List<String> dependencies, String laneId, String laneName) {
        this.nodeId = nodeId;
        this.nodeLabel = nodeLabel;
        this.nodeType = nodeType;
        this.order = order;
        this.dependencies = dependencies;
        this.laneId = laneId;
        this.laneName = laneName;
    }

    // Getters and setters
    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }

    public String getNodeLabel() { return nodeLabel; }
    public void setNodeLabel(String nodeLabel) { this.nodeLabel = nodeLabel; }

    public String getNodeType() { return nodeType; }
    public void setNodeType(String nodeType) { this.nodeType = nodeType; }

    public int getOrder() { return order; }
    public void setOrder(int order) { this.order = order; }

    public List<String> getDependencies() { return dependencies; }
    public void setDependencies(List<String> dependencies) { this.dependencies = dependencies; }

    public String getLaneId() { return laneId; }
    public void setLaneId(String laneId) { this.laneId = laneId; }

    public String getLaneName() { return laneName; }
    public void setLaneName(String laneName) { this.laneName = laneName; }
}