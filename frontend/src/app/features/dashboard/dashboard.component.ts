import { Component, OnInit } from '@angular/core';

import { DashboardStats, TASK_STATUSES, TaskStatus } from '../../core/models/models';
import { AuthService } from '../../core/services/auth.service';
import { DashboardService } from '../../core/services/dashboard.service';

interface StatCard {
  label: string;
  value: number;
  icon: string;
  color: string;
}

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html'
})
export class DashboardComponent implements OnInit {
  stats: DashboardStats | null = null;
  cards: StatCard[] = [];
  loading = true;
  readonly statuses = TASK_STATUSES;

  constructor(private readonly dashboard: DashboardService, readonly auth: AuthService) {}

  ngOnInit(): void {
    this.dashboard.stats().subscribe({
      next: stats => {
        this.stats = stats;
        this.cards = this.buildCards(stats);
        this.loading = false;
      },
      error: () => (this.loading = false)
    });
  }

  count(status: TaskStatus): number {
    return this.stats?.tasksByStatus[status] ?? 0;
  }

  percent(status: TaskStatus): number {
    const total = this.stats?.totalTasks ?? 0;
    return total === 0 ? 0 : Math.round((this.count(status) / total) * 100);
  }

  barClass(status: TaskStatus): string {
    return status === 'DONE' ? 'bg-success' : status === 'IN_PROGRESS' ? 'bg-primary' : 'bg-secondary';
  }

  /** The cards differ per role (F-05). */
  private buildCards(s: DashboardStats): StatCard[] {
    if (s.role === 'ADMIN') {
      return [
        { label: 'Total users', value: s.totalUsers ?? 0, icon: 'bi-people', color: 'primary' },
        { label: 'Total tasks', value: s.totalTasks ?? 0, icon: 'bi-list-task', color: 'info' },
        { label: 'Completed tasks', value: s.completedTasks ?? 0, icon: 'bi-check2-circle', color: 'success' }
      ];
    }
    const status: StatCard[] = [
      { label: 'Pending', value: s.tasksByStatus.PENDING, icon: 'bi-hourglass', color: 'secondary' },
      { label: 'In progress', value: s.tasksByStatus.IN_PROGRESS, icon: 'bi-arrow-repeat', color: 'primary' },
      { label: 'Done', value: s.tasksByStatus.DONE, icon: 'bi-check2-circle', color: 'success' }
    ];
    return s.role === 'MANAGER'
      ? [{ label: 'My team size', value: s.teamSize ?? 0, icon: 'bi-people', color: 'warning' }, ...status]
      : status;
  }
}
