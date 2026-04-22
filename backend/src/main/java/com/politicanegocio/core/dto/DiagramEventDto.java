package com.politicanegocio.core.dto;

import java.util.HashMap;
import java.util.Map;

public class DiagramEventDto {

    private String action;
    private String cellId;
    private Map<String, Object> payload = new HashMap<>();

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getCellId() {
        return cellId;
    }

    public void setCellId(String cellId) {
        this.cellId = cellId;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }
}
