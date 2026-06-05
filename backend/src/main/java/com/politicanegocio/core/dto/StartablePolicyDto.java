package com.politicanegocio.core.dto;

import com.politicanegocio.core.model.PolicyInitialRequirement;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StartablePolicyDto {
    private String id;
    private String name;
    private String description;
    private List<PolicyInitialRequirement> initialRequirements = new ArrayList<>();
}
