import { Injectable } from '@angular/core';
import {
  HttpErrorResponse,
  HttpEvent,
  HttpHandler,
  HttpInterceptor,
  HttpRequest
} from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { ApiError } from '../models/models';
import { AuthService } from '../services/auth.service';
import { ToastService } from '../services/toast.service';

/**
 * One place that reacts to every failed HTTP call:
 *  - 401 (token missing / expired / invalid) -> log out and go to the login page
 *  - everything else -> show the backend's message in a toast (F-07: show error messages on the frontend)
 * The error is re-thrown so components can still stop their loading spinners.
 */
@Injectable()
export class ErrorInterceptor implements HttpInterceptor {
  constructor(private readonly auth: AuthService, private readonly toast: ToastService) {}

  intercept(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    return next.handle(request).pipe(
      catchError((error: HttpErrorResponse) => {
        const isLoginCall = request.url.endsWith('/auth/login');

        if (error.status === 401 && !isLoginCall) {
          // Expired or invalid token: clear the session. The login page explains why (?reason=expired).
          if (this.auth.token) {
            this.auth.logout(true);
          }
        } else if (!(error.status === 401 && isLoginCall)) {
          // A wrong password is displayed by the login form itself.
          this.toast.error(this.messageOf(error));
        }
        return throwError(() => error);
      })
    );
  }

  private messageOf(error: HttpErrorResponse): string {
    if (error.status === 0) {
      return 'Cannot reach the server. Check your connection (a free server may need up to a minute to wake up).';
    }
    // A failed PDF download arrives as a Blob, so the JSON body cannot be read synchronously.
    if (error.error instanceof Blob) {
      return error.status === 403
        ? 'You do not have permission to perform this action.'
        : `Request failed (${error.status}).`;
    }

    const body = error.error as Partial<ApiError> | null;
    if (body && body.message) {
      const details = body.fieldErrors ? Object.values(body.fieldErrors).join(' | ') : '';
      return details ? `${body.message}: ${details}` : body.message;
    }
    return `Unexpected error (${error.status}). Please try again.`;
  }
}
