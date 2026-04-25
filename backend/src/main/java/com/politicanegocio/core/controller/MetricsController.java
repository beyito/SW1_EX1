package com.politicanegocio.core.controller;

import com.politicanegocio.core.dto.PolicyTaskMetricDto;
import com.politicanegocio.core.service.MetricsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/metrics")
public class MetricsController {
    private final MetricsService metricsService;

    public MetricsController(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @GetMapping("/policy/{policyId}")
    @PreAuthorize("hasAnyAuthority('COMPANY_ADMIN','SOFTWARE_ADMIN')")
    public ResponseEntity<List<PolicyTaskMetricDto>> getPolicyMetrics(@PathVariable String policyId) {
        return ResponseEntity.ok(metricsService.getPolicyMetrics(policyId));
    }
}
