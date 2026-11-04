# Interview notes - how the important parts work

Read this once the night before. Every answer points to the file where the logic lives.

## 1. Login and JWT (F-01)
* `AuthController.login` -> `AuthService.login` -> `AuthenticationManager.authenticate`. Spring loads the user through
  `CustomUserDetailsService` and compares the password with **BCrypt** (`PasswordEncoder`). Wrong credentials throw an
  `AuthenticationException`; `GlobalExceptionHandler` maps it to **401**.
* `JwtService.generateToken` signs a token (HS256) with `JWT_SECRET`; claims: subject = email, `uid`, `role`, `exp`.
* Stateless: the server keeps no session. The token is only valid if the signature is correct and `exp` is in the future.
* Frontend: `AuthService` stores `{token, role, name}` in `localStorage`, reads the `exp` claim and sets a timer that logs
  out when the token expires. `JwtInterceptor` adds the header; `ErrorInterceptor` logs out on any 401.

## 2. Where RBAC is enforced (F-02, most important)
Two layers, both on the **backend**:
1. **Role check** - `@PreAuthorize("hasRole('ADMIN')")` etc. on every controller method (`@EnableMethodSecurity` in `SecurityConfig`).
   A wrong role throws `AccessDeniedException` -> **403**.
2. **Row check** - the service decides which *rows* a role may touch (`TaskService`, `TaskSpecifications.visibleTo`):
   manager = tasks he assigned, employee = tasks assigned to him, manager may assign only to his own team, an employee may change
   only his own task. A violation -> **403**.
No token / bad token -> `JwtAuthenticationFilter` leaves the request anonymous -> `RestAuthenticationEntryPoint` -> **401**.
Hiding menu items in Angular (`NavbarComponent`, `AuthGuard`) is only convenience.

## 3. Task status workflow
`TaskStatus.next()` returns the only allowed next status (PENDING->IN_PROGRESS->DONE->null).
`TaskService.updateStatus` checks ownership, then `current.canMoveTo(new)`; otherwise **400** with a clear message.

## 4. Soft delete
`UserService.delete` sets `active=false`. `UserPrincipal.isEnabled()` returns `active`, so a deleted user cannot log in and an old
token stops working at once (the JWT filter reloads the user each request). Reason: tasks keep foreign keys to users.

## 5. Layers and DTOs
Controller (HTTP + validation + role) -> Service (business rules + transaction) -> Repository (queries). Entities never leave the
service: `UserMapper` / `TaskMapper` convert to records (`UserResponse`, `TaskResponse`). The password hash is not part of any DTO.
`spring.jpa.open-in-view=false`, so lazy loading outside a transaction is impossible by design; mapping happens inside the service.

## 6. Filtering and pagination
`TaskSpecifications` / `UserSpecifications` build the query dynamically (JPA Criteria). `findAll(spec, pageable)` gives the page.
The task query uses `fetch` joins for assignee and assigner to avoid N+1 queries (not for the count query).
User input in LIKE is escaped (`%`, `_`).

## 7. Validation and errors (F-07)
`@Valid` + jakarta annotations on request records -> `MethodArgumentNotValidException` -> **400** with `fieldErrors`.
One `@RestControllerAdvice` converts everything to `{timestamp,status,error,message,path,fieldErrors}`. Security filter errors are written by
`ApiErrorWriter` in the same shape. The Angular `ErrorInterceptor` shows `message` in a toast.

## 8. Dashboard and PDF
`DashboardService.stats` switches on the role; counts come from `GROUP BY status` queries; missing statuses are filled with 0.
`ReportService` picks the tasks (admin: all, manager: team) and `PdfReportGenerator` (OpenPDF) renders the table; the controller
returns bytes with `Content-Disposition: attachment`. The frontend downloads a `Blob` and clicks a temporary link.

## 9. CORS
`SecurityConfig.corsConfigurationSource` allows only origins in `CORS_ALLOWED_ORIGINS`, the needed methods/headers, and exposes
`Content-Disposition` (needed to read the PDF file name).

## 10. Likely questions
* *Why JWT, not sessions?* Stateless, scales, works across Netlify/Render origins. Trade-off: no server-side logout; short expiry (1 h).
* *Where is the token stored?* `localStorage` (simple; XSS risk). An HttpOnly cookie + CSRF protection would be the stronger option.
* *Why BCrypt?* Slow, salted adaptive hash; each hash contains its own salt and cost factor.
* *Why `ddl-auto=none`?* The schema is a deliverable (`schema.sql`); Hibernate must not change a production schema.
* *What would you add next?* Refresh tokens, audit log, email notifications, rate limiting on login, Flyway migrations, Testcontainers tests.
* *Known limits of this demo:* free hosting sleeps; deadline "not in the past" is checked in UTC with one day tolerance.
