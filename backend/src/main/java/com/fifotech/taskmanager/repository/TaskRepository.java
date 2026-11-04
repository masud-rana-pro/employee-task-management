package com.fifotech.taskmanager.repository;

import com.fifotech.taskmanager.entity.Task;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

    // ---- dashboard statistics: rows are [TaskStatus, Long] ----

    @Query("select t.status, count(t) from Task t group by t.status")
    List<Object[]> countGroupedByStatus();

    @Query("select t.status, count(t) from Task t where t.assignedBy.id = :userId group by t.status")
    List<Object[]> countGroupedByStatusAssignedBy(@Param("userId") Long userId);

    @Query("select t.status, count(t) from Task t where t.assignedTo.id = :userId group by t.status")
    List<Object[]> countGroupedByStatusAssignedTo(@Param("userId") Long userId);

    // ---- report data (users are fetched together to avoid N+1 queries) ----

    @EntityGraph(attributePaths = {"assignedTo", "assignedBy"})
    List<Task> findAllByOrderByCreatedAtDesc();

    /** Tasks of every employee that belongs to the given manager's team. */
    @EntityGraph(attributePaths = {"assignedTo", "assignedBy"})
    List<Task> findByAssignedTo_Manager_IdOrderByCreatedAtDesc(Long managerId);
}
