import { Component } from '@angular/core';
import { map } from 'rxjs/operators';

import { AuthService } from './core/services/auth.service';

/** Application shell: sidebar + top bar (only when logged in), the routed page and the toast notifications. */
@Component({
  selector: 'app-root',
  template: `
    <div class="app-shell" [class.has-sidebar]="loggedIn$ | async">
      <app-navbar></app-navbar>
      <main class="app-content">
        <header class="topbar d-none d-lg-flex" *ngIf="session$ | async as session">
          <div class="text-muted small">
            <i class="bi bi-calendar3 me-2"></i>{{ today | date: 'EEEE, d MMMM y' }}
          </div>
          <div class="d-flex align-items-center gap-2">
            <div class="text-end lh-sm">
              <div class="fw-semibold">{{ session.name }}</div>
              <div class="small text-muted">{{ session.role }}</div>
            </div>
            <span class="avatar">{{ initials(session.name) }}</span>
          </div>
        </header>
        <div class="container-fluid px-3 px-lg-4 py-4">
          <router-outlet></router-outlet>
        </div>
      </main>
    </div>
    <app-toast-container></app-toast-container>
  `
})
export class AppComponent {
  readonly session$ = this.auth.session$;
  readonly loggedIn$ = this.auth.session$.pipe(map(session => !!session));
  readonly today = new Date();

  constructor(private readonly auth: AuthService) {}

  /** "System Admin" -> "SA" */
  initials(name: string): string {
    return name
      .split(/\s+/)
      .filter(part => part.length > 0)
      .slice(0, 2)
      .map(part => part[0].toUpperCase())
      .join('');
  }
}
