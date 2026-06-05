package com.politicanegocio.core.controller;

import com.politicanegocio.core.dto.IntelligentReceptionRequestDto;
import com.politicanegocio.core.dto.IntelligentReceptionResponseDto;
import com.politicanegocio.core.model.User;
import com.politicanegocio.core.service.IntelligentReceptionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/execution/intelligent-reception")
public class IntelligentReceptionController {
    private final IntelligentReceptionService intelligentReceptionService;

    public IntelligentReceptionController(IntelligentReceptionService intelligentReceptionService) {
        this.intelligentReceptionService = intelligentReceptionService;
    }

    @PostMapping({"/recommend", "/start"})
    @PreAuthorize("hasAnyAuthority('CLIENT','FUNCTIONARY','COMPANY_ADMIN','SOFTWARE_ADMIN')")
    public ResponseEntity<IntelligentReceptionResponseDto> startFromNaturalLanguage(
            @RequestBody IntelligentReceptionRequestDto request,
            Authentication authentication
    ) {
        User user = authentication != null && authentication.getPrincipal() instanceof User
                ? (User) authentication.getPrincipal()
                : null;
        return ResponseEntity.ok(intelligentReceptionService.recommendPolicies(request, user));
    }
}
