/** Shared TypeScript types that mirror the backend DTOs. */

export type Role = 'ADMIN' | 'MANAGER' | 'EMPLOYEE';
export type TaskStatus = 'PENDING' | 'IN_PROGRESS' | 'DONE';
export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH';

export const ROLES: Role[] = ['ADMIN', 'MANAGER', 'EMPLOYEE'];
export const TASK_STATUSES: TaskStatus[] = ['PENDING', 'IN_PROGRESS', 'DONE'];
export const TASK_PRIORITIES: TaskPriority[] = ['LOW', 'MEDIUM', 'HIGH'];

// ---------- auth ----------

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  role: Role;
  name: string;
}

/** What we keep about the logged-in user (stored in localStorage). */
export interface Session {
  token: string;
  role: Role;
  name: string;
}

// ---------- users ----------

export interface User {
  id: number;
  fullName: string;
  email: string;
  role: Role;
  managerId: number | null;
  managerName: string | null;
  active: boolean;
  createdAt: string;
}

export interface UserSummary {
  id: number;
  fullName: string;
  email: string;
}

export interface UserRequest {
  fullName: string;
  email: string;
  password?: string;
  role: Role;
  managerId: number | null;
}

// ---------- tasks ----------

export interface Task {
  id: number;
  title: string;
  description: string | null;
  status: TaskStatus;
  priority: TaskPriority;
  deadline: string | null;
  assignedToId: number;
  assignedToName: string;
  assignedById: number;
  assignedByName: string;
  createdAt: string;
  updatedAt: string | null;
}

export interface TaskRequest {
  title: string;
  description: string | null;
  deadline: string | null;
  priority: TaskPriority;
  assignedTo: number;
}

export interface TaskFilters {
  status?: TaskStatus | '';
  priority?: TaskPriority | '';
  assigneeId?: number | '' | null;
  assignedByRole?: Role | '';
  search?: string;
}

// ---------- generic ----------

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface DashboardStats {
  role: Role;
  totalUsers?: number;
  totalTasks?: number;
  completedTasks?: number;
  teamSize?: number;
  tasksByStatus: Record<TaskStatus, number>;
}

/** Error body produced by the backend's GlobalExceptionHandler. */
export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  fieldErrors?: Record<string, string>;
}
