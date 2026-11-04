import { Component } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import { Role } from '../../core/models/models';
import { AuthService } from '../../core/services/auth.service';

interface NavLink {
  label: string;
  path: string;
  icon: string;
  exact?: boolean;
}

/** Top navigation bar: role specific links, the user's name and the logout button (page 8 of the spec). */
@Component({
  selector: 'app-navbar',
  templateUrl: './navbar.component.html'
})
export class NavbarComponent {
  readonly session$ = this.auth.session$;
  /**
   * The menu links are computed ONCE per login/logout (not in the template).
   * A method call in *ngFor that returns a new array on every change detection makes Angular
   * re-create all <a routerLinkActive> elements again and again - an endless loop that freezes the page.
   */
  readonly links$: Observable<NavLink[]> = this.auth.session$.pipe(
    map(session => (session ? this.linksFor(session.role) : []))
  );
  collapsed = true;

  constructor(private readonly auth: AuthService) {}

  trackByPath(_index: number, link: NavLink): string {
    return link.path;
  }

  /** The menu is only a convenience - every API call is checked again on the backend. */
  linksFor(role: Role): NavLink[] {
    switch (role) {
      case 'ADMIN':
        return [
          { label: 'Dashboard', path: '/dashboard', icon: 'bi-speedometer2' },
          { label: 'Users', path: '/users', icon: 'bi-people' },
          { label: 'Tasks', path: '/tasks', icon: 'bi-list-task', exact: true },
          { label: 'New task', path: '/tasks/new', icon: 'bi-plus-circle' },
          { label: 'Reports', path: '/reports', icon: 'bi-file-earmark-pdf' }
        ];
      case 'MANAGER':
        return [
          { label: 'Dashboard', path: '/dashboard', icon: 'bi-speedometer2' },
          { label: 'Tasks', path: '/tasks', icon: 'bi-list-task', exact: true },
          { label: 'Assign task', path: '/tasks/new', icon: 'bi-plus-circle' },
          { label: 'Reports', path: '/reports', icon: 'bi-file-earmark-pdf' }
        ];
      default:
        return [
          { label: 'Dashboard', path: '/dashboard', icon: 'bi-speedometer2' },
          { label: 'My tasks', path: '/my-tasks', icon: 'bi-check2-square' }
        ];
    }
  }

  logout(): void {
    this.auth.logout();
  }
}
