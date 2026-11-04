import { Component, Input } from '@angular/core';

import { TaskPriority, TaskStatus } from '../../core/models/models';

/** Colored pill for a task status: <app-status-badge [status]="task.status"></app-status-badge> */
@Component({
  selector: 'app-status-badge',
  template: `<span class="badge rounded-pill" [ngClass]="cssClass">{{ label }}</span>`
})
export class StatusBadgeComponent {
  @Input() status: TaskStatus = 'PENDING';

  get label(): string {
    return this.status.replace('_', ' ');
  }

  get cssClass(): string {
    switch (this.status) {
      case 'IN_PROGRESS':
        return 'text-bg-primary';
      case 'DONE':
        return 'text-bg-success';
      default:
        return 'text-bg-secondary';
    }
  }
}

/** Colored pill for a task priority. */
@Component({
  selector: 'app-priority-badge',
  template: `<span class="badge" [ngClass]="cssClass">{{ priority }}</span>`
})
export class PriorityBadgeComponent {
  @Input() priority: TaskPriority = 'MEDIUM';

  get cssClass(): string {
    switch (this.priority) {
      case 'HIGH':
        return 'text-bg-danger';
      case 'LOW':
        return 'text-bg-info';
      default:
        return 'text-bg-warning';
    }
  }
}
