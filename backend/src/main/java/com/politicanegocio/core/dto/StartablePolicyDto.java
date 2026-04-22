package com.politicanegocio.core.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StartablePolicyDto {
    private String id;
    private String name;
    private String description;
}
