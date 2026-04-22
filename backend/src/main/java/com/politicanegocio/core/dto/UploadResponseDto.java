package com.politicanegocio.core.dto;

public class UploadResponseDto {

    private String key;
    private String url;
    private String contentType;
    private long size;

    public UploadResponseDto() {
    }

    public UploadResponseDto(String key, String url, String contentType, long size) {
        this.key = key;
        this.url = url;
        this.contentType = contentType;
        this.size = size;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }
}
