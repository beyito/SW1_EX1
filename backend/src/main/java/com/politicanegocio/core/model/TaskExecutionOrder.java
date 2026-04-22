package com.politicanegocio.core.model;

import java.util.List;

public class TaskExecutionOrder {
    private String policyId;
    private String policyName;
    private List<TaskOrder> tasks;

    public TaskExecutionOrder() {}

    public TaskExecutionOrder(String policyId, String policyName, List<TaskOrder> tasks) {
        this.policyId = policyId;
        this.policyName = policyName;
        this.tasks = tasks;
    }

    // Getters and setters
    public String getPolicyId() { return policyId; }
    public void setPolicyId(String policyId) { this.policyId = policyId; }

    public String getPolicyName() { return policyName; }
    public void setPolicyName(String policyName) { this.policyName = policyName; }

    public List<TaskOrder> getTasks() { return tasks; }
    public void setTasks(List<TaskOrder> tasks) { this.tasks = tasks; }
}