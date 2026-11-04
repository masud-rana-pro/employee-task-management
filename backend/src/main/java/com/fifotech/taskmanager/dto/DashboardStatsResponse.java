package com.fifotech.taskmanager.dto;

import java.util.Map;

/**
 * Role based dashboard numbers. Fields that do not apply to a role are null and therefore
 * omitted from the JSON:
 * <ul>
 *   <li>ADMIN: totalUsers, totalTasks, completedTasks, tasksByStatus</li>
 *   <li>MANAGER: teamSize, totalTasks, tasksByStatus (tasks the manager assigned)</li>
 *   <li>EMPLOYEE: totalTasks, tasksByStatus (tasks assigned to the employee)</li>
 * </ul>
 */
public record DashboardStatsResponse(
        String role,
        Long totalUsers,
        Long totalTasks,
        Long completedTasks,
        Long teamSize,
        Map<String, Long> tasksByStatus) {
}
