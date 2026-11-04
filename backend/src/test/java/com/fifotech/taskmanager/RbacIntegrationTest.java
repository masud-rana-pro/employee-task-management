package com.fifotech.taskmanager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fifotech.taskmanager.entity.Task;
import com.fifotech.taskmanager.entity.User;
import com.fifotech.taskmanager.enums.Role;
import com.fifotech.taskmanager.enums.TaskPriority;
import com.fifotech.taskmanager.enums.TaskStatus;
import com.fifotech.taskmanager.repository.TaskRepository;
import com.fifotech.taskmanager.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end tests of the security rules (login, 401, 403, role filtered data).
 * The whole class is @Transactional: every test starts from the data created in setUp()
 * and everything is rolled back afterwards.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RbacIntegrationTest {

    private static final String PASSWORD = "Secret@123";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private TaskRepository taskRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private User admin;
    private User manager1;
    private User manager2;
    private User emp1;      // team of manager1
    private User emp2;      // team of manager1
    private User emp3;      // team of manager2
    private Task task1;     // manager1 -> emp1
    private Task task2;     // manager2 -> emp3

    @BeforeEach
    void setUp() {
        admin = saveUser("Admin", "admin@test.com", Role.ADMIN, null);
        manager1 = saveUser("Manager One", "m1@test.com", Role.MANAGER, null);
        manager2 = saveUser("Manager Two", "m2@test.com", Role.MANAGER, null);
        emp1 = saveUser("Emp One", "e1@test.com", Role.EMPLOYEE, manager1);
        emp2 = saveUser("Emp Two", "e2@test.com", Role.EMPLOYEE, manager1);
        emp3 = saveUser("Emp Three", "e3@test.com", Role.EMPLOYEE, manager2);
        task1 = saveTask("Task for emp1", emp1, manager1);
        task2 = saveTask("Task for emp3", emp3, manager2);
    }

    // ------------------------------------------------------------ authentication (F-01)

    @Test
    void loginWithValidCredentialsReturnsTokenRoleAndName() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "admin@test.com", "password", PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.name").value("Admin"));
    }

    @Test
    void loginWithWrongPasswordReturns401() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "admin@test.com", "password", "wrong"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginWithUnknownEmailReturns401() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "nobody@test.com", "password", PASSWORD))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/tasks")).andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointWithGarbageTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/tasks").header("Authorization", "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void softDeletedUserCannotLogin() throws Exception {
        emp3.setActive(false);
        userRepository.save(emp3);

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "e3@test.com", "password", PASSWORD))))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------ RBAC on user management (F-02, F-03)

    @Test
    void onlyAdminCanListUsers() throws Exception {
        mockMvc.perform(get("/api/users").header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(6));
        mockMvc.perform(get("/api/users").header("Authorization", bearer(manager1)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/users").header("Authorization", bearer(emp1)))
                .andExpect(status().isForbidden());
    }

    @Test
    void userSearchFiltersByNameOrEmail() throws Exception {
        mockMvc.perform(get("/api/users").param("search", "manager").header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void adminCreatesUserAndPasswordIsNeverReturned() throws Exception {
        Map<String, Object> body = Map.of("fullName", "New Person", "email", "New@Test.com",
                "password", "Passw0rd!", "role", "EMPLOYEE", "managerId", manager1.getId());

        mockMvc.perform(post("/api/users").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(json(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("new@test.com"))
                .andExpect(jsonPath("$.password").doesNotExist());

        User saved = userRepository.findByEmailIgnoreCase("new@test.com").orElseThrow();
        assertThat(saved.getPassword()).startsWith("$2"); // BCrypt hash
    }

    @Test
    void createUserValidatesInputAndRejectsDuplicates() throws Exception {
        Map<String, Object> invalid = Map.of("fullName", "", "email", "not-an-email",
                "password", "123", "role", "EMPLOYEE");
        mockMvc.perform(post("/api/users").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(json(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.fullName").exists())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists());

        Map<String, Object> duplicate = Map.of("fullName", "Dup", "email", "e1@test.com",
                "password", "Passw0rd!", "role", "EMPLOYEE");
        mockMvc.perform(post("/api/users").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(json(duplicate)))
                .andExpect(status().isConflict());
    }

    @Test
    void managerAndEmployeeCannotCreateUsers() throws Exception {
        Map<String, Object> body = Map.of("fullName", "X Y", "email", "xy@test.com",
                "password", "Passw0rd!", "role", "EMPLOYEE");
        mockMvc.perform(post("/api/users").header("Authorization", bearer(manager1))
                        .contentType(MediaType.APPLICATION_JSON).content(json(body)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/auth/register").header("Authorization", bearer(emp1))
                        .contentType(MediaType.APPLICATION_JSON).content(json(body)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deletingUserIsSoftAndAdminCannotDeleteSelf() throws Exception {
        mockMvc.perform(delete("/api/users/" + emp2.getId()).header("Authorization", bearer(admin)))
                .andExpect(status().isNoContent());
        assertThat(userRepository.findById(emp2.getId()).orElseThrow().isActive()).isFalse();

        mockMvc.perform(get("/api/users/" + emp2.getId()).header("Authorization", bearer(admin)))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/users/" + admin.getId()).header("Authorization", bearer(admin)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void managerWithTeamCannotBeDeleted() throws Exception {
        mockMvc.perform(delete("/api/users/" + manager1.getId()).header("Authorization", bearer(admin)))
                .andExpect(status().isConflict());
    }

    // ------------------------------------------------------------ task creation rules (F-04)

    @Test
    void managerCanAssignTaskToOwnTeamMember() throws Exception {
        mockMvc.perform(post("/api/tasks").header("Authorization", bearer(manager1))
                        .contentType(MediaType.APPLICATION_JSON).content(taskBody("New task", emp2.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.assignedToId").value(emp2.getId()))
                .andExpect(jsonPath("$.assignedById").value(manager1.getId()));
    }

    @Test
    void managerCannotAssignTaskToEmployeeOfAnotherTeam() throws Exception {
        mockMvc.perform(post("/api/tasks").header("Authorization", bearer(manager1))
                        .contentType(MediaType.APPLICATION_JSON).content(taskBody("Not allowed", emp3.getId())))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanAssignTaskToAnyEmployee() throws Exception {
        mockMvc.perform(post("/api/tasks").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(taskBody("Admin task", emp3.getId())))
                .andExpect(status().isCreated());
    }

    @Test
    void employeeCannotCreateTasks() throws Exception {
        mockMvc.perform(post("/api/tasks").header("Authorization", bearer(emp1))
                        .contentType(MediaType.APPLICATION_JSON).content(taskBody("Nope", emp1.getId())))
                .andExpect(status().isForbidden());
    }

    @Test
    void taskCannotBeAssignedToAManager() throws Exception {
        mockMvc.perform(post("/api/tasks").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(taskBody("Bad", manager2.getId())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void taskValidationReturnsFieldErrors() throws Exception {
        Map<String, Object> body = Map.of("title", " ");
        mockMvc.perform(post("/api/tasks").header("Authorization", bearer(manager1))
                        .contentType(MediaType.APPLICATION_JSON).content(json(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.title").exists())
                .andExpect(jsonPath("$.fieldErrors.assignedTo").exists());
    }

    // ------------------------------------------------------------ role filtered lists

    @Test
    void taskListIsFilteredByRole() throws Exception {
        mockMvc.perform(get("/api/tasks").header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));

        mockMvc.perform(get("/api/tasks").header("Authorization", bearer(manager1)))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Task for emp1"));

        mockMvc.perform(get("/api/tasks").header("Authorization", bearer(emp1)))
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/api/tasks").header("Authorization", bearer(emp2)))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void taskListSupportsFilters() throws Exception {
        mockMvc.perform(get("/api/tasks").param("status", "DONE").header("Authorization", bearer(admin)))
                .andExpect(jsonPath("$.totalElements").value(0));
        mockMvc.perform(get("/api/tasks").param("status", "PENDING").header("Authorization", bearer(admin)))
                .andExpect(jsonPath("$.totalElements").value(2));
        mockMvc.perform(get("/api/tasks").param("assigneeId", String.valueOf(emp3.getId()))
                        .header("Authorization", bearer(admin)))
                .andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(get("/api/tasks").param("search", "emp1").header("Authorization", bearer(admin)))
                .andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(get("/api/tasks").param("assignedByRole", "ADMIN").header("Authorization", bearer(admin)))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void invalidFilterValueReturns400() throws Exception {
        mockMvc.perform(get("/api/tasks").param("status", "UNKNOWN").header("Authorization", bearer(admin)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void singleTaskIsOnlyVisibleToItsParticipantsAndAdmin() throws Exception {
        String url = "/api/tasks/" + task1.getId();
        mockMvc.perform(get(url).header("Authorization", bearer(admin))).andExpect(status().isOk());
        mockMvc.perform(get(url).header("Authorization", bearer(manager1))).andExpect(status().isOk());
        mockMvc.perform(get(url).header("Authorization", bearer(emp1))).andExpect(status().isOk());
        mockMvc.perform(get(url).header("Authorization", bearer(manager2))).andExpect(status().isForbidden());
        mockMvc.perform(get(url).header("Authorization", bearer(emp2))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/tasks/999999").header("Authorization", bearer(admin)))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------ status workflow

    @Test
    void employeeMovesOwnTaskForwardStepByStep() throws Exception {
        String url = "/api/tasks/" + task1.getId() + "/status";

        mockMvc.perform(put(url).header("Authorization", bearer(emp1))
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("status", "IN_PROGRESS"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        // skipping or going backwards is not allowed
        mockMvc.perform(put(url).header("Authorization", bearer(emp1))
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("status", "PENDING"))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put(url).header("Authorization", bearer(emp1))
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("status", "DONE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"));

        // DONE is final
        mockMvc.perform(put(url).header("Authorization", bearer(emp1))
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("status", "IN_PROGRESS"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void employeeCannotChangeSomeoneElsesTask() throws Exception {
        mockMvc.perform(put("/api/tasks/" + task1.getId() + "/status").header("Authorization", bearer(emp2))
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("status", "IN_PROGRESS"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void managerAndAdminCannotUseTheEmployeeStatusEndpoint() throws Exception {
        String url = "/api/tasks/" + task1.getId() + "/status";
        mockMvc.perform(put(url).header("Authorization", bearer(manager1))
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("status", "IN_PROGRESS"))))
                .andExpect(status().isForbidden());
        mockMvc.perform(put(url).header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("status", "IN_PROGRESS"))))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------ edit / delete

    @Test
    void managerCanOnlyEditOwnTasksAndEmployeeCannotEdit() throws Exception {
        String url = "/api/tasks/" + task1.getId();
        mockMvc.perform(put(url).header("Authorization", bearer(manager1))
                        .contentType(MediaType.APPLICATION_JSON).content(taskBody("Renamed", emp2.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Renamed"))
                .andExpect(jsonPath("$.assignedToId").value(emp2.getId()));

        mockMvc.perform(put(url).header("Authorization", bearer(manager2))
                        .contentType(MediaType.APPLICATION_JSON).content(taskBody("Hack", emp3.getId())))
                .andExpect(status().isForbidden());

        mockMvc.perform(put(url).header("Authorization", bearer(emp1))
                        .contentType(MediaType.APPLICATION_JSON).content(taskBody("Hack", emp1.getId())))
                .andExpect(status().isForbidden());
    }

    @Test
    void onlyAdminCanDeleteTasks() throws Exception {
        String url = "/api/tasks/" + task1.getId();
        mockMvc.perform(delete(url).header("Authorization", bearer(manager1))).andExpect(status().isForbidden());
        mockMvc.perform(delete(url).header("Authorization", bearer(emp1))).andExpect(status().isForbidden());
        mockMvc.perform(delete(url).header("Authorization", bearer(admin))).andExpect(status().isNoContent());
        assertThat(taskRepository.existsById(task1.getId())).isFalse();
    }

    // ------------------------------------------------------------ dashboard (F-05)

    @Test
    void dashboardIsDifferentPerRole() throws Exception {
        mockMvc.perform(get("/api/dashboard/stats").header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.totalUsers").value(6))
                .andExpect(jsonPath("$.totalTasks").value(2))
                .andExpect(jsonPath("$.completedTasks").value(0));

        mockMvc.perform(get("/api/dashboard/stats").header("Authorization", bearer(manager1)))
                .andExpect(jsonPath("$.role").value("MANAGER"))
                .andExpect(jsonPath("$.teamSize").value(2))
                .andExpect(jsonPath("$.tasksByStatus.PENDING").value(1))
                .andExpect(jsonPath("$.totalUsers").doesNotExist());

        mockMvc.perform(get("/api/dashboard/stats").header("Authorization", bearer(emp1)))
                .andExpect(jsonPath("$.role").value("EMPLOYEE"))
                .andExpect(jsonPath("$.tasksByStatus.PENDING").value(1))
                .andExpect(jsonPath("$.tasksByStatus.DONE").value(0))
                .andExpect(jsonPath("$.teamSize").doesNotExist());
    }

    // ------------------------------------------------------------ report (PDF)

    @Test
    void pdfReportIsAvailableForAdminAndManagerOnly() throws Exception {
        MvcResult adminResult = mockMvc.perform(get("/api/reports/tasks/pdf").header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(adminResult.getResponse().getContentType()).isEqualTo("application/pdf");
        assertThat(new String(adminResult.getResponse().getContentAsByteArray(), 0, 4)).isEqualTo("%PDF");

        mockMvc.perform(get("/api/reports/tasks/pdf").header("Authorization", bearer(manager1)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/reports/tasks/pdf").header("Authorization", bearer(emp1)))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------ helpers

    private User saveUser(String name, String email, Role role, User manager) {
        User user = new User();
        user.setFullName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(PASSWORD));
        user.setRole(role);
        user.setManager(manager);
        user.setActive(true);
        return userRepository.save(user);
    }

    private Task saveTask(String title, User assignee, User assigner) {
        Task task = new Task();
        task.setTitle(title);
        task.setPriority(TaskPriority.MEDIUM);
        task.setStatus(TaskStatus.PENDING);
        task.setAssignedTo(assignee);
        task.setAssignedBy(assigner);
        return taskRepository.save(task);
    }

    /** Logs in through the real endpoint and returns the "Bearer ..." header value. */
    private String bearer(User user) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", user.getEmail(), "password", PASSWORD))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return "Bearer " + node.get("token").asText();
    }

    private String taskBody(String title, Long assigneeId) throws Exception {
        return json(Map.of(
                "title", title,
                "description", "Some description",
                "priority", "HIGH",
                "deadline", LocalDate.now().plusDays(7).toString(),
                "assignedTo", assigneeId));
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
