package com.politicanegocio.core.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Document(collection = "users")
public class User {
    @Id
    private String id;
    private String username;
    @JsonIgnore
    private String password;
    private List<String> roles = new ArrayList<>();
    private String company;
    private String parentCompany;
    private String area;
    private String laneId;
    private String fcmToken;
    private LocalDateTime fcmTokenUpdatedAt;
}
