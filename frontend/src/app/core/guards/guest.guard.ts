import { Injectable } from '@angular/core';
import { CanActivate, Router, UrlTree } from '@angular/router';

import { AuthService } from '../services/auth.service';

/** Keeps logged-in users away from the login page. */
@Injectable({ providedIn: 'root' })
export class GuestGuard implements CanActivate {
  constructor(private readonly auth: AuthService, private readonly router: Router) {}

  canActivate(): boolean | UrlTree {
    return this.auth.isLoggedIn() ? this.router.createUrlTree(['/dashboard']) : true;
  }
}
