import { Component } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';

import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html'
})
export class LoginComponent {
  readonly form = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]]
  });

  loading = false;
  errorMessage = '';
  readonly sessionExpired = this.route.snapshot.queryParamMap.get('reason') === 'expired';

  constructor(
    private readonly fb: FormBuilder,
    private readonly auth: AuthService,
    private readonly router: Router,
    private readonly route: ActivatedRoute
  ) {}

  invalid(name: 'email' | 'password'): boolean {
    const c = this.form.get(name);
    return !!c && c.touched && c.invalid;
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading = true;
    this.errorMessage = '';

    this.auth.login({ email: this.form.value.email, password: this.form.value.password }).subscribe({
      next: () => {
        const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
        // only allow in-app paths as redirect target
        this.router.navigateByUrl(returnUrl && returnUrl.startsWith('/') ? returnUrl : '/dashboard');
      },
      error: err => {
        this.loading = false;
        this.errorMessage =
          err.status === 401
            ? 'Invalid email or password.'
            : err.status === 0
              ? 'Cannot reach the server. A free server can take up to a minute to wake up - please try again.'
              : 'Login failed. Please try again.';
      }
    });
  }
}
