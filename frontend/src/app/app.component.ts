import { Component } from '@angular/core';
import { map } from 'rxjs/operators';

import { AuthService } from './core/services/auth.service';

/** Application shell: sidebar (only when logged in), the routed page and the toast notifications. */
@Component({
  selector: 'app-root',
  template: `
    <div class="app-shell" [class.has-sidebar]="loggedIn$ | async">
      <app-navbar></app-navbar>
      <main class="app-content">
        <div class="container-fluid px-3 px-lg-4 py-4">
          <router-outlet></router-outlet>
        </div>
      </main>
    </div>
    <app-toast-container></app-toast-container>
  `
})
export class AppComponent {
  readonly loggedIn$ = this.auth.session$.pipe(map(session => !!session));

  constructor(private readonly auth: AuthService) {}
}
