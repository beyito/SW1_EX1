package com.politicanegocio.core.dto;

import java.util.Map;

public class OnlyOfficeConfigDto {
    private String documentServerUrl;
    private Map<String, Object> config;

    public OnlyOfficeConfigDto(String documentServerUrl, Map<String, Object> config) {
        this.documentServerUrl = documentServerUrl;
        this.config = config;
    }

    public String getDocumentServerUrl() {
        return documentServerUrl;
    }

    public void setDocumentServerUrl(String documentServerUrl) {
        this.documentServerUrl = documentServerUrl;
    }

    public Map<String, Object> getConfig() {
        return config;
    }

    public void setConfig(Map<String, Object> config) {
        this.config = config;
    }
}
