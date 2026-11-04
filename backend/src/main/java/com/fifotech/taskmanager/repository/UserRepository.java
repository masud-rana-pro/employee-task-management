package com.fifotech.taskmanager.repository;

import com.fifotech.taskmanager.entity.User;
import com.fifotech.taskmanager.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> findByEmailIgnoreCase(String email);

    Optional<User> findByIdAndActiveTrue(Long id);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    /** Dashboard: number of active users. */
    long countByActiveTrue();

    /** Manager dashboard: size of the team (active employees reporting to the manager). */
    long countByManager_IdAndActiveTrue(Long managerId);

    /** Used to refuse deleting / demoting a manager who still has a team. */
    boolean existsByManager_IdAndActiveTrue(Long managerId);

    /** Drop-down data: all active users of one role (admin assigns to any employee). */
    List<User> findByRoleAndActiveTrueOrderByFullNameAsc(Role role);

    /** Drop-down data: the active employees of one manager. */
    List<User> findByManager_IdAndRoleAndActiveTrueOrderByFullNameAsc(Long managerId, Role role);
}
