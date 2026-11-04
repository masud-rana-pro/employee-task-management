import { Component } from '@angular/core';

/** Application shell: navigation bar, the routed page and the toast notifications. */
@Component({
  selector: 'app-root',
  template: `
    <app-navbar></app-navbar>
    <main class="container py-4">
      <router-outlet></router-outlet>
    </main>
    <app-toast-container></app-toast-container>
  `
})
export class AppComponent {}
