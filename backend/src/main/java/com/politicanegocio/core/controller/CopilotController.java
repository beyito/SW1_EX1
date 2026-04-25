package com.politicanegocio.core.controller;

import com.politicanegocio.core.dto.CopilotRequestDto;
import com.politicanegocio.core.dto.CopilotApplyRequestDto;
import com.politicanegocio.core.dto.CopilotApplyResponseDto;
import com.politicanegocio.core.dto.CopilotResponseDto;
import com.politicanegocio.core.service.CopilotService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/copilot")
public class CopilotController {
    private static final Logger log = LoggerFactory.getLogger(CopilotController.class);
    private final CopilotService copilotService;

    public CopilotController(CopilotService copilotService) {
        this.copilotService = copilotService;
    }

    @PostMapping("/chat")
    @PreAuthorize("hasAnyAuthority('COMPANY_ADMIN','SOFTWARE_ADMIN')")
    public ResponseEntity<CopilotResponseDto> chat(@RequestBody CopilotRequestDto request) {
        log.info(
                "CopilotController.chat request: messageLength={} hasDiagram={}",
                request != null && request.userMessage() != null ? request.userMessage().length() : 0,
                request != null && request.currentDiagram() != null
        );
        return ResponseEntity.ok(copilotService.chat(request));
    }

    @PostMapping("/apply")
    @PreAuthorize("hasAnyAuthority('COMPANY_ADMIN','SOFTWARE_ADMIN')")
    public ResponseEntity<CopilotApplyResponseDto> apply(@RequestBody CopilotApplyRequestDto request) {
        log.info(
                "CopilotController.apply request: instructionLength={} hasDiagram={}",
                request != null && request.instruction() != null ? request.instruction().length() : 0,
                request != null && request.currentDiagram() != null
        );
        return ResponseEntity.ok(copilotService.apply(request));
    }
}
