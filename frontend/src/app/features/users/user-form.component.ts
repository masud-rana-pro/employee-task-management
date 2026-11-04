import { Component, Input, OnInit } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';

import { Role, ROLES, User, UserRequest, UserSummary } from '../../core/models/models';
import { UserService } from '../../core/services/user.service';

/** Add / Edit modal (page 3 of the spec). */
@Component({
  selector: 'app-user-form',
  templateUrl: './user-form.component.html'
})
export class UserFormComponent implements OnInit {
  /** null = create mode, a user = edit mode. */
  @Input() user: User | null = null;

  readonly roles = ROLES;
  managers: UserSummary[] = [];
  saving = false;

  readonly form = this.fb.group({
    fullName: ['', [Validators.required, Validators.maxLength(100)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(150)]],
    password: ['', [Validators.minLength(6), Validators.maxLength(100)]],
    role: ['EMPLOYEE' as Role, Validators.required],
    managerId: [null as number | null]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly userService: UserService,
    readonly modal: NgbActiveModal
  ) {}

  get isEdit(): boolean {
    return this.user !== null;
  }

  get isEmployee(): boolean {
    return this.form.get('role')?.value === 'EMPLOYEE';
  }

  ngOnInit(): void {
    if (this.user) {
      this.form.patchValue({
        fullName: this.user.fullName,
        email: this.user.email,
        role: this.user.role,
        managerId: this.user.managerId
      });
    } else {
      // password is mandatory when creating
      this.form.get('password')?.addValidators(Validators.required);
    }
    this.userService.managers().subscribe(list => (this.managers = list));
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue() as {
      fullName: string; email: string; password: string; role: Role; managerId: number | null;
    };
    const request: UserRequest = {
      fullName: v.fullName.trim(),
      email: v.email.trim(),
      role: v.role,
      managerId: v.role === 'EMPLOYEE' ? v.managerId : null
    };
    if (v.password) {
      request.password = v.password;
    }

    this.saving = true;
    const call = this.user
      ? this.userService.update(this.user.id, request)
      : this.userService.create(request);
    call.subscribe({
      next: saved => this.modal.close(saved),
      error: () => (this.saving = false) // message shown by the error interceptor
    });
  }

  invalid(name: 'fullName' | 'email' | 'password'): boolean {
    const c = this.form.get(name);
    return !!c && c.touched && c.invalid;
  }
}
