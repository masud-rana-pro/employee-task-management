import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { PageResponse, User, UserRequest, UserSummary } from '../models/models';

/** Calls the /api/users endpoints. */
@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly url = `${environment.apiUrl}/users`;

  constructor(private readonly http: HttpClient) {}

  /** @param page zero based page index (the backend convention) */
  list(search: string, page: number, size: number): Observable<PageResponse<User>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (search.trim()) {
      params = params.set('search', search.trim());
    }
    return this.http.get<PageResponse<User>>(this.url, { params });
  }

  get(id: number): Observable<User> {
    return this.http.get<User>(`${this.url}/${id}`);
  }

  create(request: UserRequest): Observable<User> {
    return this.http.post<User>(this.url, request);
  }

  update(id: number, request: UserRequest): Observable<User> {
    return this.http.put<User>(`${this.url}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }

  /** Employees the current user may assign tasks to (admin: all, manager: own team). */
  assignees(): Observable<UserSummary[]> {
    return this.http.get<UserSummary[]>(`${this.url}/assignees`);
  }

  /** All managers (drop-down in the user form). */
  managers(): Observable<UserSummary[]> {
    return this.http.get<UserSummary[]>(`${this.url}/managers`);
  }
}
