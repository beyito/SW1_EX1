package com.politicanegocio.core.controller;

import com.politicanegocio.core.dto.CopilotRequestDto;
import com.politicanegocio.core.dto.CopilotApplyRequestDto;
import com.politicanegocio.core.dto.CopilotApplyResponseDto;
import com.politicanegocio.core.dto.CopilotConversationDto;
import com.politicanegocio.core.dto.CopilotResponseDto;
import com.politicanegocio.core.dto.VoiceFillRequestDto;
import com.politicanegocio.core.model.User;
import com.politicanegocio.core.service.CopilotService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.Map;

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
    public ResponseEntity<CopilotResponseDto> chat(@RequestBody CopilotRequestDto request, Authentication authentication) {
        User actor = authentication != null && authentication.getPrincipal() instanceof User
                ? (User) authentication.getPrincipal()
                : null;
        log.info(
                "CopilotController.chat request: messageLength={} hasDiagram={} actor={}",
                request != null && request.userMessage() != null ? request.userMessage().length() : 0,
                request != null && request.currentDiagram() != null,
                actor != null ? actor.getUsername() : "unknown"
        );
        return ResponseEntity.ok(copilotService.chat(request, actor));
    }

    @GetMapping("/history")
    @PreAuthorize("hasAnyAuthority('COMPANY_ADMIN','SOFTWARE_ADMIN')")
    public ResponseEntity<CopilotConversationDto> history(
            @RequestParam(required = false) String policyId,
            @RequestParam(required = false) String conversationId,
            Authentication authentication
    ) {
        User actor = authentication != null && authentication.getPrincipal() instanceof User
                ? (User) authentication.getPrincipal()
                : null;
        return ResponseEntity.ok(copilotService.getConversationHistory(actor, policyId, conversationId));
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

    @PostMapping("/voice-fill")
    @PreAuthorize("hasAnyAuthority('COMPANY_ADMIN','SOFTWARE_ADMIN','FUNCTIONARY','CLIENT')")
    public ResponseEntity<Map<String, Object>> voiceFill(@RequestBody VoiceFillRequestDto request) {
        return ResponseEntity.ok(copilotService.fillFormFromVoice(request));
    }
}
