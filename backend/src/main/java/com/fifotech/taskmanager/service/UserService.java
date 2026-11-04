package com.fifotech.taskmanager.service;

import com.fifotech.taskmanager.dto.CreateUserRequest;
import com.fifotech.taskmanager.dto.PageResponse;
import com.fifotech.taskmanager.dto.UpdateUserRequest;
import com.fifotech.taskmanager.dto.UserResponse;
import com.fifotech.taskmanager.dto.UserSummary;
import com.fifotech.taskmanager.entity.User;
import com.fifotech.taskmanager.enums.Role;
import com.fifotech.taskmanager.exception.BadRequestException;
import com.fifotech.taskmanager.exception.ConflictException;
import com.fifotech.taskmanager.exception.ResourceNotFoundException;
import com.fifotech.taskmanager.mapper.UserMapper;
import com.fifotech.taskmanager.repository.UserRepository;
import com.fifotech.taskmanager.repository.UserSpecifications;
import com.fifotech.taskmanager.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * User management business logic (the controller of this feature is ADMIN only).
 * Deleting a user is a SOFT delete (active = false) so the task history stays intact.
 */
@Service
@Transactional(readOnly = true)
public class UserService {

    private static final int MAX_PAGE_SIZE = 100;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ------------------------------------------------------------------ queries

    public PageResponse<UserResponse> list(String search, int page, int size) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.ASC, "fullName").and(Sort.by(Sort.Direction.ASC, "id")));

        Page<User> result = userRepository.findAll(UserSpecifications.activeMatching(search), pageable);
        return PageResponse.from(result.map(UserMapper::toResponse));
    }

    public UserResponse get(Long id) {
        return UserMapper.toResponse(findActiveUser(id));
    }

    /**
     * Employees that the caller may assign tasks to:
     * an ADMIN gets every employee, a MANAGER only the employees of his/her own team.
     */
    public List<UserSummary> assignableEmployees(UserPrincipal actor) {
        List<User> employees = switch (actor.getRole()) {
            case ADMIN -> userRepository.findByRoleAndActiveTrueOrderByFullNameAsc(Role.EMPLOYEE);
            case MANAGER -> userRepository.findByManager_IdAndRoleAndActiveTrueOrderByFullNameAsc(
                    actor.getId(), Role.EMPLOYEE);
            case EMPLOYEE -> throw new AccessDeniedException("Employees cannot assign tasks");
        };
        return employees.stream().map(UserMapper::toSummary).toList();
    }

    /** All active managers - used by the admin form to pick the manager of an employee. */
    public List<UserSummary> managers() {
        return userRepository.findByRoleAndActiveTrueOrderByFullNameAsc(Role.MANAGER)
                .stream().map(UserMapper::toSummary).toList();
    }

    // ------------------------------------------------------------------ commands

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("A user with this email already exists");
        }

        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password())); // BCrypt hash, never the raw password
        user.setRole(request.role());
        user.setManager(resolveManager(request.role(), request.managerId(), null));
        user.setActive(true);

        return UserMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse update(Long id, UpdateUserRequest request, UserPrincipal actor) {
        User user = findActiveUser(id);

        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new ConflictException("A user with this email already exists");
        }

        // Protect the system from being locked out.
        if (user.getId().equals(actor.getId()) && request.role() != user.getRole()) {
            throw new BadRequestException("You cannot change your own role");
        }
        // A manager who still leads a team cannot become something else.
        if (user.getRole() == Role.MANAGER && request.role() != Role.MANAGER
                && userRepository.existsByManager_IdAndActiveTrue(user.getId())) {
            throw new ConflictException("This manager still has team members. Reassign them first");
        }

        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setRole(request.role());
        user.setManager(resolveManager(request.role(), request.managerId(), user.getId()));
        if (request.password() != null && !request.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }

        return UserMapper.toResponse(userRepository.save(user));
    }

    /** Soft delete: the row stays, the user can no longer log in and disappears from the lists. */
    @Transactional
    public void delete(Long id, UserPrincipal actor) {
        User user = findActiveUser(id);

        if (user.getId().equals(actor.getId())) {
            throw new BadRequestException("You cannot delete your own account");
        }
        if (user.getRole() == Role.MANAGER && userRepository.existsByManager_IdAndActiveTrue(user.getId())) {
            throw new ConflictException("This manager still has team members. Reassign them first");
        }

        user.setActive(false);
        userRepository.save(user);
    }

    // ------------------------------------------------------------------ helpers

    private User findActiveUser(Long id) {
        return userRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + id));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    /**
     * Only an EMPLOYEE may have a manager, and that manager must be an active user with role MANAGER.
     *
     * @param userId id of the user being edited (null when creating) - a user can never be his own manager
     */
    private User resolveManager(Role role, Long managerId, Long userId) {
        if (managerId == null) {
            return null;
        }
        if (role != Role.EMPLOYEE) {
            throw new BadRequestException("Only employees can be assigned to a manager");
        }
        if (managerId.equals(userId)) {
            throw new BadRequestException("A user cannot be his/her own manager");
        }
        User manager = userRepository.findByIdAndActiveTrue(managerId)
                .orElseThrow(() -> new BadRequestException("Manager not found with id " + managerId));
        if (manager.getRole() != Role.MANAGER) {
            throw new BadRequestException("The selected user is not a manager");
        }
        return manager;
    }
}
