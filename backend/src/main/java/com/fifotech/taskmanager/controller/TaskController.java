package com.fifotech.taskmanager.controller;

import com.fifotech.taskmanager.dto.PageResponse;
import com.fifotech.taskmanager.dto.TaskRequest;
import com.fifotech.taskmanager.dto.TaskResponse;
import com.fifotech.taskmanager.dto.UpdateTaskStatusRequest;
import com.fifotech.taskmanager.enums.Role;
import com.fifotech.taskmanager.enums.TaskPriority;
import com.fifotech.taskmanager.enums.TaskStatus;
import com.fifotech.taskmanager.security.UserPrincipal;
import com.fifotech.taskmanager.service.TaskService;
import com.fifotech.taskmanager.service.TaskService.TaskFilter;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    /** MANAGER (own team only) or ADMIN (any employee) creates and assigns a task. */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse create(@Valid @RequestBody TaskRequest request,
                               @AuthenticationPrincipal UserPrincipal principal) {
        return taskService.create(request, principal);
    }

    /**
     * Role-filtered, paginated list. Optional filters: status, priority, assigneeId, search (title),
     * and - useful for admins - assignedById / assignedByRole.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','EMPLOYEE')")
    public PageResponse<TaskResponse> list(@RequestParam(required = false) TaskStatus status,
                                           @RequestParam(required = false) TaskPriority priority,
                                           @RequestParam(required = false) Long assigneeId,
                                           @RequestParam(required = false) Long assignedById,
                                           @RequestParam(required = false) Role assignedByRole,
                                           @RequestParam(required = false) String search,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "10") int size,
                                           @AuthenticationPrincipal UserPrincipal principal) {
        TaskFilter filter = new TaskFilter(status, priority, assigneeId, assignedById, assignedByRole, search);
        return taskService.list(filter, principal, page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','EMPLOYEE')")
    public TaskResponse get(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        return taskService.get(id, principal);
    }

    /** EMPLOYEE only, own task only: PENDING -> IN_PROGRESS -> DONE. */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public TaskResponse updateStatus(@PathVariable Long id,
                                     @Valid @RequestBody UpdateTaskStatusRequest request,
                                     @AuthenticationPrincipal UserPrincipal principal) {
        return taskService.updateStatus(id, request.status(), principal);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public TaskResponse update(@PathVariable Long id,
                               @Valid @RequestBody TaskRequest request,
                               @AuthenticationPrincipal UserPrincipal principal) {
        return taskService.update(id, request, principal);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        taskService.delete(id);
    }
}
