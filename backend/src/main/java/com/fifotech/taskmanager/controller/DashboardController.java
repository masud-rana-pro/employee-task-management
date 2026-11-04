package com.fifotech.taskmanager.controller;

import com.fifotech.taskmanager.dto.DashboardStatsResponse;
import com.fifotech.taskmanager.security.UserPrincipal;
import com.fifotech.taskmanager.service.DashboardService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /** One endpoint, three different answers depending on the caller's role. */
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','EMPLOYEE')")
    public DashboardStatsResponse stats(@AuthenticationPrincipal UserPrincipal principal) {
        return dashboardService.stats(principal);
    }
}
