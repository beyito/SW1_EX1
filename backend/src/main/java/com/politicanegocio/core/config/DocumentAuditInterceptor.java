package com.politicanegocio.core.config;

import com.politicanegocio.core.model.DocumentAction;
import com.politicanegocio.core.model.User;
import com.politicanegocio.core.service.DocumentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Map;

@Component
public class DocumentAuditInterceptor implements HandlerInterceptor {
    private final DocumentService documentService;

    public DocumentAuditInterceptor(DocumentService documentService) {
        this.documentService = documentService;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception exception
    ) {
        if (response.getStatus() >= 400) {
            return;
        }

        String documentId = resolveDocumentId(request);
        if (documentId == null || documentId.isBlank()) {
            return;
        }

        DocumentAction action = resolveAction(request);
        if (action == null) {
            return;
        }

        documentService.logAction(resolveUser(), action, documentId, request.getMethod(), request.getRequestURI());
    }

    private DocumentAction resolveAction(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        if ("GET".equalsIgnoreCase(method) && path.endsWith("/editor-config")) {
            return DocumentAction.EDIT;
        }
        if ("GET".equalsIgnoreCase(method)) {
            return DocumentAction.VIEW;
        }
        if ("POST".equalsIgnoreCase(method)) {
            return DocumentAction.EDIT;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private String resolveDocumentId(HttpServletRequest request) {
        Object attributes = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (attributes instanceof Map<?, ?> variables) {
            Object value = variables.get("documentId");
            return value != null ? value.toString() : null;
        }
        return null;
    }

    private User resolveUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User user) {
            return user;
        }
        return null;
    }
}
