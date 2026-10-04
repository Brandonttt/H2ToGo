package com.h2togo.backend.admin;

import com.h2togo.backend.admin.dto.DashboardResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Panorama operativo para el panel web de administración. Rol ADMIN (SecurityConfig). */
@RestController
@RequestMapping("/api/v1/admin/dashboard")
public class AdminDashboardController {

    private final AdminDashboardService service;

    public AdminDashboardController(AdminDashboardService service) {
        this.service = service;
    }

    @GetMapping
    public DashboardResponse resumen() {
        return service.resumen();
    }
}
