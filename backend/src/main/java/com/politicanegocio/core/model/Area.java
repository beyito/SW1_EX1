package com.politicanegocio.core.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "areas")
public class Area {
    @Id
    private String id;
    private String name;
    private String company;
}
