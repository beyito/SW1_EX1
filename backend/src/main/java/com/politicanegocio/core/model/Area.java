package com.politicanegocio.core.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Data
@Document(collection = "areas")
public class Area {
    @Id
    private String id;
    private String name;
    private String company;
    private List<String> streets = new ArrayList<>();
}
