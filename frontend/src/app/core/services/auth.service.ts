import { Injectable, NgZone } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable } from 'rxjs';
import { tap } from 'rxjs/operators';

import { environment } from '../../../environments/environment';
import { LoginRequest, LoginResponse, Role, Session } from '../models/models';

const STORAGE_KEY = 'tms_session';
/** setTimeout cannot handle delays above 2^31-1 ms (about 24 days). */
const MAX_TIMER_MS = 2_147_483_647;

/**
 * Keeps the login state of the browser:
 *  - login() calls the API and stores { token, role, name } in localStorage
 *  - logout() clears it and goes back to the login page
 *  - a timer logs the user out automatically when the JWT expires (requirement F-01)
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly sessionSubject = new BehaviorSubject<Session | null>(this.readStoredSession());
  /** Emits the current session (or null when logged out) - the navbar listens to this. */
  readonly session$ = this.sessionSubject.asObservable();

  private expiryTimer: ReturnType<typeof setTimeout> | null = null;

  constructor(
    private readonly http: HttpClient,
    private readonly router: Router,
    private readonly zone: NgZone
  ) {
    // A page reload must keep the auto-logout timer alive.
    if (this.sessionSubject.value) {
      this.scheduleAutoLogout(this.sessionSubject.value.token);
    }
  }

  // ------------------------------------------------------------------ API calls

  login(credentials: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${environment.apiUrl}/auth/login`, credentials).pipe(
      tap(response => {
        const session: Session = { token: response.token, role: response.role, name: response.name };
        localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
        this.sessionSubject.next(session);
        this.scheduleAutoLogout(session.token);
      })
    );
  }

  /** @param expired true when the logout happens because the session ran out (adds a hint to the URL) */
  logout(expired = false): void {
    this.clearExpiryTimer();
    localStorage.removeItem(STORAGE_KEY);
    this.sessionSubject.next(null);
    this.router.navigate(['/login'], expired ? { queryParams: { reason: 'expired' } } : {});
  }

  // ------------------------------------------------------------------ state helpers

  get token(): string | null {
    return this.sessionSubject.value?.token ?? null;
  }

  get role(): Role | null {
    return this.sessionSubject.value?.role ?? null;
  }

  get userName(): string {
    return this.sessionSubject.value?.name ?? '';
  }

  /** True when a token exists and has not expired yet. */
  isLoggedIn(): boolean {
    const token = this.token;
    if (!token) {
      return false;
    }
    const expiresAt = this.getExpiryTime(token);
    return expiresAt === null || expiresAt > Date.now();
  }

  hasRole(roles: Role[]): boolean {
    const role = this.role;
    return role !== null && roles.includes(role);
  }

  // ------------------------------------------------------------------ internals

  private readStoredSession(): Session | null {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      if (!raw) {
        return null;
      }
      const session = JSON.parse(raw) as Session;
      const expiresAt = this.getExpiryTime(session.token);
      if (!session.token || (expiresAt !== null && expiresAt <= Date.now())) {
        localStorage.removeItem(STORAGE_KEY);
        return null;
      }
      return session;
    } catch {
      localStorage.removeItem(STORAGE_KEY);
      return null;
    }
  }

  private scheduleAutoLogout(token: string): void {
    this.clearExpiryTimer();
    const expiresAt = this.getExpiryTime(token);
    if (expiresAt === null) {
      return;
    }
    const delay = Math.min(Math.max(expiresAt - Date.now(), 0), MAX_TIMER_MS);
    // Run outside Angular so the timer does not keep the zone "unstable"; re-enter it for the logout.
    this.zone.runOutsideAngular(() => {
      this.expiryTimer = setTimeout(() => this.zone.run(() => this.logout(true)), delay);
    });
  }

  private clearExpiryTimer(): void {
    if (this.expiryTimer !== null) {
      clearTimeout(this.expiryTimer);
      this.expiryTimer = null;
    }
  }

  /** Reads the "exp" claim (seconds since epoch) of a JWT and returns it in milliseconds. */
  private getExpiryTime(token: string): number | null {
    try {
      const payload = token.split('.')[1];
      const json = atob(payload.replace(/-/g, '+').replace(/_/g, '/'));
      const exp = (JSON.parse(json) as { exp?: number }).exp;
      return typeof exp === 'number' ? exp * 1000 : null;
    } catch {
      return null;
    }
  }
}
