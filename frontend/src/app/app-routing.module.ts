import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { AuthGuard } from './core/guards/auth.guard';
import { GuestGuard } from './core/guards/guest.guard';

/**
 * Every feature is a lazy-loaded module. AuthGuard checks "is logged in" and, when the route declares
 * data.roles, "has one of these roles".
 */
const routes: Routes = [
  {
    path: 'login',
    canActivate: [GuestGuard],
    loadChildren: () => import('./features/auth/auth.module').then(m => m.AuthModule)
  },
  {
    path: 'dashboard',
    canActivate: [AuthGuard],
    loadChildren: () => import('./features/dashboard/dashboard.module').then(m => m.DashboardModule)
  },
  {
    path: 'users',
    canActivate: [AuthGuard],
    data: { roles: ['ADMIN'] },
    loadChildren: () => import('./features/users/users.module').then(m => m.UsersModule)
  },
  {
    path: 'tasks',
    canActivate: [AuthGuard],
    data: { roles: ['ADMIN', 'MANAGER'] },
    loadChildren: () => import('./features/tasks/tasks.module').then(m => m.TasksModule)
  },
  {
    path: 'my-tasks',
    canActivate: [AuthGuard],
    data: { roles: ['EMPLOYEE'] },
    loadChildren: () => import('./features/my-tasks/my-tasks.module').then(m => m.MyTasksModule)
  },
  {
    path: 'reports',
    canActivate: [AuthGuard],
    data: { roles: ['ADMIN', 'MANAGER'] },
    loadChildren: () => import('./features/reports/reports.module').then(m => m.ReportsModule)
  },
  { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
  { path: '**', redirectTo: 'dashboard' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule {}
