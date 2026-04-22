package com.politicanegocio.core.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "companies")
public class Company {
    @Id
    private String id;
    private String name;
    private String createdBy;
}
