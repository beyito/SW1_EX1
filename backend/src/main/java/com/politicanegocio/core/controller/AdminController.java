package com.politicanegocio.core.controller;

import com.politicanegocio.core.model.Area;
import com.politicanegocio.core.model.Company;
import com.politicanegocio.core.model.User;
import com.politicanegocio.core.service.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PostMapping("/companies")
    @PreAuthorize("hasAuthority('SOFTWARE_ADMIN')")
    public ResponseEntity<Company> createCompany(@RequestBody CreateCompanyRequest request, Authentication authentication) {
        User user = getCurrentUser(authentication);
        Company company = adminService.createCompany(request.name(), user);
        return ResponseEntity.ok(company);
    }

    @GetMapping("/companies")
    @PreAuthorize("hasAuthority('SOFTWARE_ADMIN')")
    public ResponseEntity<List<Company>> listCompanies() {
        return ResponseEntity.ok(adminService.listCompanies());
    }

    @PostMapping("/company-admins")
    @PreAuthorize("hasAuthority('SOFTWARE_ADMIN')")
    public ResponseEntity<User> createCompanyAdmin(@RequestBody CreateCompanyAdminRequest request, Authentication authentication) {
        User user = getCurrentUser(authentication);
        User createdUser = adminService.createCompanyAdmin(request.username(), request.password(), request.company(), user);
        return ResponseEntity.ok(createdUser);
    }

    @PostMapping("/areas")
    @PreAuthorize("hasAuthority('COMPANY_ADMIN')")
    public ResponseEntity<Area> createArea(@RequestBody CreateAreaRequest request, Authentication authentication) {
        User user = getCurrentUser(authentication);
        Area area = adminService.createArea(request.name(), user.getCompany(), request.streets(), user);
        return ResponseEntity.ok(area);
    }

    @GetMapping("/areas")
    @PreAuthorize("hasAuthority('COMPANY_ADMIN')")
    public ResponseEntity<List<Area>> listAreas(Authentication authentication) {
        User user = getCurrentUser(authentication);
        return ResponseEntity.ok(adminService.listCompanyAreas(user.getCompany()));
    }

    @PostMapping("/functionaries")
    @PreAuthorize("hasAuthority('COMPANY_ADMIN')")
    public ResponseEntity<User> createFunctionary(@RequestBody CreateFunctionaryRequest request, Authentication authentication) {
        User user = getCurrentUser(authentication);
        User createdUser = adminService.createFunctionary(request.username(), request.password(), user.getCompany(), request.area(), user);
        return ResponseEntity.ok(createdUser);
    }

    @GetMapping("/functionaries")
    @PreAuthorize("hasAuthority('COMPANY_ADMIN')")
    public ResponseEntity<List<User>> listFunctionaries(Authentication authentication) {
        User user = getCurrentUser(authentication);
        return ResponseEntity.ok(adminService.listCompanyFunctionaries(user.getCompany()));
    }

    private User getCurrentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new IllegalStateException("Usuario no autenticado");
        }
        return user;
    }

    private record CreateCompanyRequest(String name) {}
    private record CreateCompanyAdminRequest(String username, String password, String company) {}
    private record CreateAreaRequest(String name, List<String> streets) {}
    private record CreateFunctionaryRequest(String username, String password, String area) {}
}
