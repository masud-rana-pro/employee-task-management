package com.fifotech.taskmanager.controller;

import com.fifotech.taskmanager.dto.CreateUserRequest;
import com.fifotech.taskmanager.dto.PageResponse;
import com.fifotech.taskmanager.dto.UpdateUserRequest;
import com.fifotech.taskmanager.dto.UserResponse;
import com.fifotech.taskmanager.dto.UserSummary;
import com.fifotech.taskmanager.security.UserPrincipal;
import com.fifotech.taskmanager.service.UserService;
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

import java.util.List;

/** User management. ADMIN only, except the small "assignees" list that managers need to assign tasks. */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** Paginated list with optional search by name / email. */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<UserResponse> list(@RequestParam(required = false) String search,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "10") int size) {
        return userService.list(search, page, size);
    }

    /** Employees the caller can assign tasks to (admin: all, manager: own team). */
    @GetMapping("/assignees")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public List<UserSummary> assignees(@AuthenticationPrincipal UserPrincipal principal) {
        return userService.assignableEmployees(principal);
    }

    /** All managers, for the "manager" drop-down of the user form. */
    @GetMapping("/managers")
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserSummary> managers() {
        return userService.managers();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse get(@PathVariable Long id) {
        return userService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
        return userService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse update(@PathVariable Long id,
                               @Valid @RequestBody UpdateUserRequest request,
                               @AuthenticationPrincipal UserPrincipal principal) {
        return userService.update(id, request, principal);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        userService.delete(id, principal);
    }
}
