import { Injectable } from '@angular/core';
import { HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { AuthService } from '../services/auth.service';

/** Adds "Authorization: Bearer <token>" to every request that goes to our own API. */
@Injectable()
export class JwtInterceptor implements HttpInterceptor {
  constructor(private readonly auth: AuthService) {}

  intercept(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    const token = this.auth.token;

    // Never leak the token to another website.
    if (token && request.url.startsWith(environment.apiUrl)) {
      request = request.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
    }
    return next.handle(request);
  }
}
