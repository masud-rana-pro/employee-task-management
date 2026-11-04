package com.fifotech.taskmanager.service;

import com.fifotech.taskmanager.dto.DashboardStatsResponse;
import com.fifotech.taskmanager.enums.TaskStatus;
import com.fifotech.taskmanager.repository.TaskRepository;
import com.fifotech.taskmanager.repository.UserRepository;
import com.fifotech.taskmanager.security.UserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Builds the numbers shown on the role specific dashboards. */
@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final UserRepository userRepository;
    private final TaskRepository taskRepository;

    public DashboardService(UserRepository userRepository, TaskRepository taskRepository) {
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
    }

    public DashboardStatsResponse stats(UserPrincipal actor) {
        return switch (actor.getRole()) {
            case ADMIN -> adminStats();
            case MANAGER -> managerStats(actor);
            case EMPLOYEE -> employeeStats(actor);
        };
    }

    /** Admin: total users, total tasks, completed tasks. */
    private DashboardStatsResponse adminStats() {
        Map<String, Long> byStatus = toStatusMap(taskRepository.countGroupedByStatus());
        return new DashboardStatsResponse(
                "ADMIN",
                userRepository.countByActiveTrue(),
                sum(byStatus),
                byStatus.get(TaskStatus.DONE.name()),
                null,
                byStatus);
    }

    /** Manager: my team size, tasks (that I assigned) grouped by status. */
    private DashboardStatsResponse managerStats(UserPrincipal actor) {
        Map<String, Long> byStatus = toStatusMap(taskRepository.countGroupedByStatusAssignedBy(actor.getId()));
        return new DashboardStatsResponse(
                "MANAGER",
                null,
                sum(byStatus),
                null,
                userRepository.countByManager_IdAndActiveTrue(actor.getId()),
                byStatus);
    }

    /** Employee: my task counts grouped by status. */
    private DashboardStatsResponse employeeStats(UserPrincipal actor) {
        Map<String, Long> byStatus = toStatusMap(taskRepository.countGroupedByStatusAssignedTo(actor.getId()));
        return new DashboardStatsResponse(
                "EMPLOYEE",
                null,
                sum(byStatus),
                null,
                null,
                byStatus);
    }

    /** Turns the [status, count] rows into a map that always contains all three statuses (zero filled). */
    private Map<String, Long> toStatusMap(List<Object[]> rows) {
        Map<String, Long> result = new LinkedHashMap<>();
        for (TaskStatus status : TaskStatus.values()) {
            result.put(status.name(), 0L);
        }
        for (Object[] row : rows) {
            result.put(((TaskStatus) row[0]).name(), ((Number) row[1]).longValue());
        }
        return result;
    }

    private long sum(Map<String, Long> byStatus) {
        return byStatus.values().stream().mapToLong(Long::longValue).sum();
    }
}
