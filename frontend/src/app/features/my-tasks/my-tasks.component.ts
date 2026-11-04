import { Component, OnInit } from '@angular/core';

import { Task, TaskStatus, TASK_STATUSES } from '../../core/models/models';
import { TaskService } from '../../core/services/task.service';
import { ToastService } from '../../core/services/toast.service';

/** Employee page: own tasks + a button that moves a task one step forward. */
@Component({
  selector: 'app-my-tasks',
  templateUrl: './my-tasks.component.html'
})
export class MyTasksComponent implements OnInit {
  tasks: Task[] = [];
  loading = false;
  page = 1;
  readonly pageSize = 10;
  total = 0;
  statusFilter: TaskStatus | '' = '';
  readonly statuses = TASK_STATUSES;
  updatingId: number | null = null;

  constructor(private readonly taskService: TaskService, private readonly toast: ToastService) {}

  ngOnInit(): void {
    this.load();
  }

  filterChanged(): void {
    this.page = 1;
    this.load();
  }

  load(): void {
    this.loading = true;
    this.taskService.list({ status: this.statusFilter }, this.page - 1, this.pageSize).subscribe({
      next: res => {
        this.tasks = res.content;
        this.total = res.totalElements;
        this.loading = false;
      },
      error: () => (this.loading = false)
    });
  }

  /** The only allowed move is forward: PENDING -> IN_PROGRESS -> DONE. */
  nextStatus(task: Task): TaskStatus | null {
    return task.status === 'PENDING' ? 'IN_PROGRESS' : task.status === 'IN_PROGRESS' ? 'DONE' : null;
  }

  advance(task: Task): void {
    const next = this.nextStatus(task);
    if (!next) {
      return;
    }
    this.updatingId = task.id;
    this.taskService.updateStatus(task.id, next).subscribe({
      next: () => {
        this.updatingId = null;
        this.toast.success(`Task moved to ${next.replace('_', ' ')}.`);
        this.load();
      },
      error: () => (this.updatingId = null)
    });
  }

  buttonLabel(task: Task): string {
    return task.status === 'PENDING' ? 'Start' : 'Mark done';
  }

  isOverdue(task: Task): boolean {
    return !!task.deadline && task.status !== 'DONE' && task.deadline < new Date().toISOString().substring(0, 10);
  }
}
