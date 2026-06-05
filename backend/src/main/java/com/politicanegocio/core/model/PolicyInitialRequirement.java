package com.politicanegocio.core.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PolicyInitialRequirement {
    private String id;
    private String name;
    private String description;
    private boolean required;
    private List<String> allowedExtensions = new ArrayList<>();
}
