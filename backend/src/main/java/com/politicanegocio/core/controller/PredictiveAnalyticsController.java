package com.politicanegocio.core.controller;

import com.politicanegocio.core.dto.PredictiveAnalysisResponseDto;
import com.politicanegocio.core.model.User;
import com.politicanegocio.core.service.PredictiveAnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/predictions")
public class PredictiveAnalyticsController {
    private final PredictiveAnalyticsService predictiveAnalyticsService;

    public PredictiveAnalyticsController(PredictiveAnalyticsService predictiveAnalyticsService) {
        this.predictiveAnalyticsService = predictiveAnalyticsService;
    }

    @GetMapping("/analysis")
    @PreAuthorize("hasAuthority('COMPANY_ADMIN')")
    public ResponseEntity<PredictiveAnalysisResponseDto> analysis(Authentication authentication) {
        User user = authentication != null && authentication.getPrincipal() instanceof User
                ? (User) authentication.getPrincipal()
                : null;
        return ResponseEntity.ok(predictiveAnalyticsService.analyze(user));
    }
}
