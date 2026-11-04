package com.fifotech.taskmanager.repository;

import com.fifotech.taskmanager.entity.User;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

/** Dynamic query pieces for the user list (search box). */
public final class UserSpecifications {

    private UserSpecifications() {
    }

    /** Active users whose name or email contains the search text (case-insensitive). */
    public static Specification<User> activeMatching(String search) {
        return (root, query, cb) -> {
            // Load the manager in the same SQL query (but not for the COUNT query used by paging).
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("manager", JoinType.LEFT);
            }

            Predicate active = cb.isTrue(root.get("active"));
            if (search == null || search.isBlank()) {
                return active;
            }
            String pattern = "%" + escapeLike(search.trim().toLowerCase()) + "%";
            Predicate nameMatches = cb.like(cb.lower(root.get("fullName")), pattern, '\\');
            Predicate emailMatches = cb.like(cb.lower(root.get("email")), pattern, '\\');
            return cb.and(active, cb.or(nameMatches, emailMatches));
        };
    }

    /** Makes %, _ and \ in user input match literally. */
    static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
