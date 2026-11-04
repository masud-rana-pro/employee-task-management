import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subject } from 'rxjs';
import { debounceTime, takeUntil } from 'rxjs/operators';

import {
  Task, TaskFilters, TASK_PRIORITIES, TASK_STATUSES, UserSummary
} from '../../core/models/models';
import { AuthService } from '../../core/services/auth.service';
import { TaskService } from '../../core/services/task.service';
import { ToastService } from '../../core/services/toast.service';
import { UserService } from '../../core/services/user.service';
import { ConfirmService } from '../../shared/components/confirm-dialog.component';

/** Task list for ADMIN (all tasks) and MANAGER (tasks they assigned) - the backend applies the role filter. */
@Component({
  selector: 'app-task-list',
  templateUrl: './task-list.component.html'
})
export class TaskListComponent implements OnInit, OnDestroy {
  tasks: Task[] = [];
  assignees: UserSummary[] = [];
  loading = false;
  page = 1;
  readonly pageSize = 10;
  total = 0;

  filters: TaskFilters = { status: '', priority: '', assigneeId: '', assignedByRole: '', search: '' };
  readonly statuses = TASK_STATUSES;
  readonly priorities = TASK_PRIORITIES;

  private readonly search$ = new Subject<void>();
  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly taskService: TaskService,
    private readonly userService: UserService,
    private readonly confirm: ConfirmService,
    private readonly toast: ToastService,
    readonly auth: AuthService
  ) {}

  get isAdmin(): boolean {
    return this.auth.role === 'ADMIN';
  }

  ngOnInit(): void {
    this.search$.pipe(debounceTime(300), takeUntil(this.destroy$)).subscribe(() => this.applyFilters());
    this.userService.assignees().subscribe(list => (this.assignees = list));
    this.load();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  onSearchTyped(): void {
    this.search$.next();
  }

  applyFilters(): void {
    this.page = 1;
    this.load();
  }

  reset(): void {
    this.filters = { status: '', priority: '', assigneeId: '', assignedByRole: '', search: '' };
    this.applyFilters();
  }

  load(): void {
    this.loading = true;
    this.taskService.list(this.filters, this.page - 1, this.pageSize).subscribe({
      next: res => {
        this.tasks = res.content;
        this.total = res.totalElements;
        this.loading = false;
      },
      error: () => (this.loading = false)
    });
  }

  async remove(task: Task): Promise<void> {
    const ok = await this.confirm.confirm(`Delete the task "${task.title}"? This cannot be undone.`, 'Delete task');
    if (!ok) {
      return;
    }
    this.taskService.delete(task.id).subscribe(() => {
      this.toast.success('Task deleted.');
      if (this.tasks.length === 1 && this.page > 1) {
        this.page--;
      }
      this.load();
    });
  }

  isOverdue(task: Task): boolean {
    return !!task.deadline && task.status !== 'DONE' && task.deadline < new Date().toISOString().substring(0, 10);
  }
}
