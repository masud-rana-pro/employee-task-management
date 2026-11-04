-- =====================================================================
--  Mini Employee & Task Management System  -  MySQL 8.x schema + seed data
--  Database : taskdb
--  Safe to run more than once (CREATE ... IF NOT EXISTS / INSERT IGNORE).
--
--  CLOUD VERSION: identical to schema.sql but without CREATE DATABASE / USE.
--  Run it inside the database your provider created for you (e.g. Aiven "defaultdb").
--  Safe to run more than once.
-- =====================================================================


-- ---------------------------------------------------------------------
-- TABLE: users
-- manager_id is a self reference: an EMPLOYEE points to his/her MANAGER.
-- "active" = 0 means the user was (soft) deleted and can no longer log in.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    full_name   VARCHAR(100) NOT NULL,
    email       VARCHAR(150) NOT NULL,
    password    VARCHAR(255) NOT NULL,                       -- BCrypt hash
    role        ENUM('ADMIN','MANAGER','EMPLOYEE') NOT NULL,
    manager_id  BIGINT       NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT fk_users_manager FOREIGN KEY (manager_id) REFERENCES users (id)
        ON DELETE SET NULL,
    INDEX idx_users_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
-- (MySQL creates an index for the manager_id foreign key automatically.)

-- ---------------------------------------------------------------------
-- TABLE: tasks
-- assigned_to = the employee who does the task
-- assigned_by = the manager / admin who created it
-- Deleting a user is a soft delete, so these foreign keys are RESTRICT.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS tasks (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    title        VARCHAR(200) NOT NULL,
    description  TEXT         NULL,
    status       ENUM('PENDING','IN_PROGRESS','DONE')  NOT NULL DEFAULT 'PENDING',
    priority     ENUM('LOW','MEDIUM','HIGH')           NOT NULL DEFAULT 'MEDIUM',
    deadline     DATE         NULL,
    assigned_to  BIGINT       NOT NULL,
    assigned_by  BIGINT       NOT NULL,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_tasks PRIMARY KEY (id),
    CONSTRAINT fk_tasks_assigned_to FOREIGN KEY (assigned_to) REFERENCES users (id),
    CONSTRAINT fk_tasks_assigned_by FOREIGN KEY (assigned_by) REFERENCES users (id),
    INDEX idx_tasks_status   (status),
    INDEX idx_tasks_priority (priority)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
-- (assigned_to / assigned_by are indexed automatically by their foreign keys.)

-- ---------------------------------------------------------------------
-- SEED DATA
-- Passwords are BCrypt hashes (cost 10):
--   admin@demo.com    Admin@123     ADMIN
--   manager@demo.com  Manager@123   MANAGER
--   emp1@demo.com     Emp@123       EMPLOYEE (team of manager@demo.com)
--   emp2@demo.com     Emp@123       EMPLOYEE (team of manager@demo.com)
-- ---------------------------------------------------------------------
INSERT IGNORE INTO users (id, full_name, email, password, role, manager_id, active) VALUES
    (1, 'System Admin',   'admin@demo.com',   '$2a$10$MpDgnvjz99ZDSTwXu/3ijO4JIuJK6Luk07JJN0hI5ra4fd61PMRWG', 'ADMIN',    NULL, TRUE),
    (2, 'Maria Manager',  'manager@demo.com', '$2a$10$uvcGegyUQz77eBx7eanJTu1OqaHru81mLZ5cUNo/P/S4vyU56pK6y', 'MANAGER',  NULL, TRUE),
    (3, 'Eddie Employee', 'emp1@demo.com',    '$2a$10$4zUzUe7v9JXDbYA36VFdPuKuT0lKi3WZ1aPR02xuIUnSVeOUTED1e', 'EMPLOYEE', 2,    TRUE),
    (4, 'Emma Employee',  'emp2@demo.com',    '$2a$10$4zUzUe7v9JXDbYA36VFdPuKuT0lKi3WZ1aPR02xuIUnSVeOUTED1e', 'EMPLOYEE', 2,    TRUE);

INSERT IGNORE INTO tasks (id, title, description, status, priority, deadline, assigned_to, assigned_by) VALUES
    (1, 'Prepare monthly sales report',  'Collect figures from all branches and prepare the summary sheet.', 'PENDING',     'HIGH',   CURDATE() + INTERVAL 5  DAY, 3, 2),
    (2, 'Update customer database',      'Remove duplicate customer records and fix missing phone numbers.', 'IN_PROGRESS', 'MEDIUM', CURDATE() + INTERVAL 10 DAY, 3, 2),
    (3, 'Write user manual',             'Document the login and task workflow for new colleagues.',         'DONE',        'LOW',    CURDATE() - INTERVAL 2  DAY, 3, 2),
    (4, 'Test the new login page',       'Check validation messages and the redirect after login.',          'PENDING',     'MEDIUM', CURDATE() + INTERVAL 3  DAY, 4, 2),
    (5, 'Organise team meeting',         'Book a room and send the agenda one day before.',                  'IN_PROGRESS', 'LOW',    CURDATE() + INTERVAL 7  DAY, 4, 2),
    (6, 'Archive last year invoices',    'Move the scanned invoices to the archive folder.',                 'DONE',        'HIGH',   CURDATE() - INTERVAL 5  DAY, 4, 1);
