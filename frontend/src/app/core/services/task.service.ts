import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { PageResponse, Task, TaskFilters, TaskRequest, TaskStatus } from '../models/models';

/** Calls the /api/tasks endpoints. */
@Injectable({ providedIn: 'root' })
export class TaskService {
  private readonly url = `${environment.apiUrl}/tasks`;

  constructor(private readonly http: HttpClient) {}

  /** The backend already filters by role; these filters only narrow the result further. */
  list(filters: TaskFilters, page: number, size: number): Observable<PageResponse<Task>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (filters.status) {
      params = params.set('status', filters.status);
    }
    if (filters.priority) {
      params = params.set('priority', filters.priority);
    }
    if (filters.assigneeId) {
      params = params.set('assigneeId', filters.assigneeId);
    }
    if (filters.assignedByRole) {
      params = params.set('assignedByRole', filters.assignedByRole);
    }
    if (filters.search && filters.search.trim()) {
      params = params.set('search', filters.search.trim());
    }
    return this.http.get<PageResponse<Task>>(this.url, { params });
  }

  get(id: number): Observable<Task> {
    return this.http.get<Task>(`${this.url}/${id}`);
  }

  create(request: TaskRequest): Observable<Task> {
    return this.http.post<Task>(this.url, request);
  }

  update(id: number, request: TaskRequest): Observable<Task> {
    return this.http.put<Task>(`${this.url}/${id}`, request);
  }

  /** Employee only: moves the task one step forward. */
  updateStatus(id: number, status: TaskStatus): Observable<Task> {
    return this.http.put<Task>(`${this.url}/${id}/status`, { status });
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }
}
