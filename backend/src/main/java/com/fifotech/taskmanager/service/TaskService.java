package com.fifotech.taskmanager.service;

import com.fifotech.taskmanager.dto.PageResponse;
import com.fifotech.taskmanager.dto.TaskRequest;
import com.fifotech.taskmanager.dto.TaskResponse;
import com.fifotech.taskmanager.entity.Task;
import com.fifotech.taskmanager.entity.User;
import com.fifotech.taskmanager.enums.Role;
import com.fifotech.taskmanager.enums.TaskPriority;
import com.fifotech.taskmanager.enums.TaskStatus;
import com.fifotech.taskmanager.exception.BadRequestException;
import com.fifotech.taskmanager.exception.ResourceNotFoundException;
import com.fifotech.taskmanager.mapper.TaskMapper;
import com.fifotech.taskmanager.repository.TaskRepository;
import com.fifotech.taskmanager.repository.TaskSpecifications;
import com.fifotech.taskmanager.repository.UserRepository;
import com.fifotech.taskmanager.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * Task business rules. The controller decides WHICH ROLE may call a use case (@PreAuthorize),
 * this class decides WHICH ROWS that role may touch:
 * <ul>
 *   <li>ADMIN: any task</li>
 *   <li>MANAGER: only tasks he/she assigned, and only to employees of his/her own team</li>
 *   <li>EMPLOYEE: only tasks assigned to him/her (and only the status can be changed)</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class TaskService {

    private static final int MAX_PAGE_SIZE = 100;

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public TaskService(TaskRepository taskRepository, UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    /** Optional filters of the task list (null = no filter). */
    public record TaskFilter(TaskStatus status, TaskPriority priority, Long assigneeId,
                             Long assignedById, Role assignedByRole, String search) {
    }

    // ------------------------------------------------------------------ queries

    public PageResponse<TaskResponse> list(TaskFilter filter, UserPrincipal actor, int page, int size) {
        // 1. users are fetched with the task, 2. the role decides which rows are visible, 3. optional filters
        Specification<Task> spec = TaskSpecifications.withUsers()
                .and(TaskSpecifications.visibleTo(actor.getRole(), actor.getId()));

        if (filter.status() != null) {
            spec = spec.and(TaskSpecifications.hasStatus(filter.status()));
        }
        if (filter.priority() != null) {
            spec = spec.and(TaskSpecifications.hasPriority(filter.priority()));
        }
        if (filter.assigneeId() != null) {
            spec = spec.and(TaskSpecifications.assignedToUser(filter.assigneeId()));
        }
        if (filter.assignedById() != null) {
            spec = spec.and(TaskSpecifications.assignedByUser(filter.assignedById()));
        }
        if (filter.assignedByRole() != null) {
            spec = spec.and(TaskSpecifications.assignedByRole(filter.assignedByRole()));
        }
        if (filter.search() != null && !filter.search().isBlank()) {
            spec = spec.and(TaskSpecifications.titleContains(filter.search()));
        }

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id")));

        Page<Task> result = taskRepository.findAll(spec, pageable);
        return PageResponse.from(result.map(TaskMapper::toResponse));
    }

    public TaskResponse get(Long id, UserPrincipal actor) {
        Task task = findTask(id);
        assertCanView(task, actor);
        return TaskMapper.toResponse(task);
    }

    // ------------------------------------------------------------------ commands

    @Transactional
    public TaskResponse create(TaskRequest request, UserPrincipal actor) {
        User assignee = resolveAssignee(request.assignedTo(), actor);
        validateDeadline(request.deadline(), null);

        Task task = new Task();
        task.setTitle(request.title().trim());
        task.setDescription(blankToNull(request.description()));
        task.setDeadline(request.deadline());
        task.setPriority(request.priority() != null ? request.priority() : TaskPriority.MEDIUM);
        task.setStatus(TaskStatus.PENDING);
        task.setAssignedTo(assignee);
        task.setAssignedBy(userRepository.getReferenceById(actor.getId()));

        return TaskMapper.toResponse(taskRepository.save(task));
    }

    /** Full edit by the manager who created the task (or by an admin). The status is NOT editable here. */
    @Transactional
    public TaskResponse update(Long id, TaskRequest request, UserPrincipal actor) {
        Task task = findTask(id);

        if (actor.getRole() == Role.MANAGER && !task.getAssignedBy().getId().equals(actor.getId())) {
            throw new AccessDeniedException("You can only edit tasks that you assigned");
        }
        User assignee = resolveAssignee(request.assignedTo(), actor);
        validateDeadline(request.deadline(), task.getDeadline());

        task.setTitle(request.title().trim());
        task.setDescription(blankToNull(request.description()));
        task.setDeadline(request.deadline());
        if (request.priority() != null) {
            task.setPriority(request.priority());
        }
        task.setAssignedTo(assignee);

        return TaskMapper.toResponse(taskRepository.save(task));
    }

    /** Employee moves his/her own task one step forward: PENDING -> IN_PROGRESS -> DONE. */
    @Transactional
    public TaskResponse updateStatus(Long id, TaskStatus newStatus, UserPrincipal actor) {
        Task task = findTask(id);

        if (!task.getAssignedTo().getId().equals(actor.getId())) {
            throw new AccessDeniedException("You can only update the status of your own tasks");
        }

        TaskStatus current = task.getStatus();
        if (!current.canMoveTo(newStatus)) {
            String message = current.next() == null
                    ? "This task is already DONE and cannot be changed"
                    : "Invalid status change " + current + " -> " + newStatus
                      + ". The next allowed status is " + current.next();
            throw new BadRequestException(message);
        }

        task.setStatus(newStatus);
        return TaskMapper.toResponse(taskRepository.save(task));
    }

    @Transactional
    public void delete(Long id) {
        Task task = findTask(id);
        taskRepository.delete(task);
    }

    // ------------------------------------------------------------------ helpers

    private Task findTask(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id " + id));
    }

    /** Same visibility rule as the list, applied to a single task. */
    private void assertCanView(Task task, UserPrincipal actor) {
        boolean allowed = switch (actor.getRole()) {
            case ADMIN -> true;
            case MANAGER -> task.getAssignedBy().getId().equals(actor.getId());
            case EMPLOYEE -> task.getAssignedTo().getId().equals(actor.getId());
        };
        if (!allowed) {
            throw new AccessDeniedException("You are not allowed to view this task");
        }
    }

    /**
     * The assignee must be an active EMPLOYEE.
     * A MANAGER may only pick employees of his/her own team; an ADMIN may pick any employee.
     */
    private User resolveAssignee(Long assigneeId, UserPrincipal actor) {
        User assignee = userRepository.findByIdAndActiveTrue(assigneeId)
                .orElseThrow(() -> new BadRequestException("Assignee not found with id " + assigneeId));

        if (assignee.getRole() != Role.EMPLOYEE) {
            throw new BadRequestException("Tasks can only be assigned to employees");
        }
        if (actor.getRole() == Role.MANAGER) {
            boolean inOwnTeam = assignee.getManager() != null
                    && assignee.getManager().getId().equals(actor.getId());
            if (!inOwnTeam) {
                throw new AccessDeniedException("You can only assign tasks to employees in your own team");
            }
        }
        return assignee;
    }

    /**
     * A new deadline must not be in the past. An unchanged old deadline is accepted so that an
     * overdue task can still be edited. One day of tolerance covers users west of UTC.
     */
    private void validateDeadline(LocalDate newDeadline, LocalDate currentDeadline) {
        if (newDeadline == null || newDeadline.equals(currentDeadline)) {
            return;
        }
        if (newDeadline.isBefore(LocalDate.now(ZoneOffset.UTC).minusDays(1))) {
            throw new BadRequestException("Deadline cannot be in the past");
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
