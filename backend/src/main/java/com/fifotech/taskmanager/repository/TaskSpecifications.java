package com.fifotech.taskmanager.repository;

import com.fifotech.taskmanager.entity.Task;
import com.fifotech.taskmanager.enums.Role;
import com.fifotech.taskmanager.enums.TaskPriority;
import com.fifotech.taskmanager.enums.TaskStatus;
import org.springframework.data.jpa.domain.Specification;

/** Building blocks for the dynamic task list query (role scope + search filters). */
public final class TaskSpecifications {

    private TaskSpecifications() {
    }

    /** Starting point: loads assignee and assigner together with the task (avoids N+1 queries). */
    public static Specification<Task> withUsers() {
        return (root, query, cb) -> {
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("assignedTo");
                root.fetch("assignedBy");
            }
            return cb.conjunction();
        };
    }

    /**
     * The ROLE based visibility rule - the heart of "role-filtered list":
     * ADMIN sees everything, MANAGER only tasks he/she assigned, EMPLOYEE only tasks assigned to him/her.
     */
    public static Specification<Task> visibleTo(Role role, Long userId) {
        return switch (role) {
            case ADMIN -> (root, query, cb) -> cb.conjunction();
            case MANAGER -> (root, query, cb) -> cb.equal(root.get("assignedBy").get("id"), userId);
            case EMPLOYEE -> (root, query, cb) -> cb.equal(root.get("assignedTo").get("id"), userId);
        };
    }

    public static Specification<Task> hasStatus(TaskStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Task> hasPriority(TaskPriority priority) {
        return (root, query, cb) -> cb.equal(root.get("priority"), priority);
    }

    public static Specification<Task> assignedToUser(Long userId) {
        return (root, query, cb) -> cb.equal(root.get("assignedTo").get("id"), userId);
    }

    public static Specification<Task> assignedByUser(Long userId) {
        return (root, query, cb) -> cb.equal(root.get("assignedBy").get("id"), userId);
    }

    /** Admin filter "by role": tasks created by managers vs. tasks created by admins. */
    public static Specification<Task> assignedByRole(Role role) {
        return (root, query, cb) -> cb.equal(root.get("assignedBy").get("role"), role);
    }

    public static Specification<Task> titleContains(String text) {
        String pattern = "%" + UserSpecifications.escapeLike(text.trim().toLowerCase()) + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("title")), pattern, '\\');
    }
}
