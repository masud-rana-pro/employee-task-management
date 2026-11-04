import { Component, OnDestroy, OnInit } from '@angular/core';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, takeUntil } from 'rxjs/operators';

import { User } from '../../core/models/models';
import { UserService } from '../../core/services/user.service';
import { ToastService } from '../../core/services/toast.service';
import { ConfirmService } from '../../shared/components/confirm-dialog.component';
import { UserFormComponent } from './user-form.component';

@Component({
  selector: 'app-user-list',
  templateUrl: './user-list.component.html'
})
export class UserListComponent implements OnInit, OnDestroy {
  users: User[] = [];
  loading = false;
  search = '';
  /** ng-bootstrap pagination is 1-based, the API is 0-based. */
  page = 1;
  readonly pageSize = 10;
  total = 0;

  private readonly search$ = new Subject<string>();
  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly userService: UserService,
    private readonly modal: NgbModal,
    private readonly confirm: ConfirmService,
    private readonly toast: ToastService
  ) {}

  ngOnInit(): void {
    this.search$.pipe(debounceTime(300), distinctUntilChanged(), takeUntil(this.destroy$)).subscribe(() => {
      this.page = 1;
      this.load();
    });
    this.load();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  onSearch(value: string): void {
    this.search = value;
    this.search$.next(value);
  }

  load(): void {
    this.loading = true;
    this.userService.list(this.search, this.page - 1, this.pageSize).subscribe({
      next: res => {
        this.users = res.content;
        this.total = res.totalElements;
        this.loading = false;
      },
      error: () => (this.loading = false)
    });
  }

  openForm(user?: User): void {
    const ref = this.modal.open(UserFormComponent, { centered: true, backdrop: 'static' });
    ref.componentInstance.user = user ?? null;
    ref.result.then(
      () => {
        this.toast.success(user ? 'User updated.' : 'User created.');
        this.load();
      },
      () => undefined // dismissed
    );
  }

  async remove(user: User): Promise<void> {
    const ok = await this.confirm.confirm(
      `Delete ${user.fullName}? The user will no longer be able to log in. Their task history is kept.`,
      'Delete user'
    );
    if (!ok) {
      return;
    }
    this.userService.delete(user.id).subscribe(() => {
      this.toast.success('User deleted.');
      // step back one page when the last row of the last page was removed
      if (this.users.length === 1 && this.page > 1) {
        this.page--;
      }
      this.load();
    });
  }

  roleClass(role: string): string {
    return role === 'ADMIN' ? 'text-bg-danger' : role === 'MANAGER' ? 'text-bg-warning' : 'text-bg-secondary';
  }
}
