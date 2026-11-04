import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivate, Router, RouterStateSnapshot, UrlTree } from '@angular/router';

import { Role } from '../models/models';
import { AuthService } from '../services/auth.service';
import { ToastService } from '../services/toast.service';

/**
 * Route guard used for every page after login.
 *  1. not logged in (or token expired)  -> redirect to /login
 *  2. route has data: { roles: [...] } and the user's role is not in the list -> redirect to /dashboard
 *
 * NOTE: this only improves the user experience. The real protection is on the backend (@PreAuthorize).
 */
@Injectable({ providedIn: 'root' })
export class AuthGuard implements CanActivate {
  constructor(
    private readonly auth: AuthService,
    private readonly router: Router,
    private readonly toast: ToastService
  ) {}

  canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): boolean | UrlTree {
    if (!this.auth.isLoggedIn()) {
      return this.router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
    }

    const allowedRoles = route.data['roles'] as Role[] | undefined;
    if (allowedRoles && !this.auth.hasRole(allowedRoles)) {
      this.toast.warning('You do not have access to that page.');
      return this.router.createUrlTree(['/dashboard']);
    }
    return true;
  }
}
