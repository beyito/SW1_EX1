package com.politicanegocio.core.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper; // <-- NUEVA IMPORTACIÓN
import com.politicanegocio.core.dto.OnlyOfficeConfigDto;
import com.politicanegocio.core.model.User;
import com.politicanegocio.core.service.OnlyOfficeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/onlyoffice")
public class OnlyOfficeController {
    
    private final OnlyOfficeService onlyOfficeService;
    private final ObjectMapper objectMapper; // <-- 1. DECLARAMOS EL MAPPER

    // 2. LO INYECTAMOS EN EL CONSTRUCTOR
    public OnlyOfficeController(OnlyOfficeService onlyOfficeService, ObjectMapper objectMapper) {
        this.onlyOfficeService = onlyOfficeService;
        this.objectMapper = objectMapper; 
    }

    @GetMapping("/config/{documentId}")
    public ResponseEntity<OnlyOfficeConfigDto> getEditorConfig(
            @PathVariable String documentId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(onlyOfficeService.buildEditorConfig(documentId, currentUser(authentication)));
    }

    @PostMapping("/callback/{documentId}")
    public ResponseEntity<Map<String, Integer>> callback(
            @PathVariable String documentId,
            @RequestBody Map<String, Object> rawPayload // <-- 3. EL PARCHE SALVAVIDAS
    ) {
        // 4. Convertimos el Map seguro a JsonNode para que tu Servicio lo lea sin problemas
        JsonNode payload = objectMapper.valueToTree(rawPayload);
        
        onlyOfficeService.handleSaveCallback(documentId, payload);
        return ResponseEntity.ok(Map.of("error", 0));
    }

    private User currentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            return null;
        }
        return user;
    }
}