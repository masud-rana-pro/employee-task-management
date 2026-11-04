package com.fifotech.taskmanager.service;

import com.fifotech.taskmanager.entity.Task;
import com.fifotech.taskmanager.repository.TaskRepository;
import com.fifotech.taskmanager.security.UserPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Chooses WHICH tasks go into the report for the caller's role, then asks the generator to render them:
 * ADMIN -> every task, MANAGER -> tasks of the employees in his/her own team.
 */
@Service
@Transactional(readOnly = true)
public class ReportService {

    private final TaskRepository taskRepository;
    private final PdfReportGenerator pdfReportGenerator;

    public ReportService(TaskRepository taskRepository, PdfReportGenerator pdfReportGenerator) {
        this.taskRepository = taskRepository;
        this.pdfReportGenerator = pdfReportGenerator;
    }

    public byte[] generateTaskReport(UserPrincipal actor) {
        List<Task> tasks;
        String scope;

        switch (actor.getRole()) {
            case ADMIN -> {
                tasks = taskRepository.findAllByOrderByCreatedAtDesc();
                scope = "All tasks";
            }
            case MANAGER -> {
                tasks = taskRepository.findByAssignedTo_Manager_IdOrderByCreatedAtDesc(actor.getId());
                scope = "Tasks of the team of " + actor.getFullName();
            }
            default -> throw new AccessDeniedException("Reports are available for admins and managers only");
        }

        return pdfReportGenerator.generate(scope, actor.getFullName(), tasks);
    }
}
