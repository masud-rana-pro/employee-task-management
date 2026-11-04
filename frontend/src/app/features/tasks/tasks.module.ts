import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { TaskFormComponent } from './task-form.component';
import { TaskListComponent } from './task-list.component';

@NgModule({
  declarations: [TaskListComponent, TaskFormComponent],
  imports: [
    SharedModule,
    RouterModule.forChild([
      { path: '', component: TaskListComponent },
      { path: 'new', component: TaskFormComponent },
      { path: ':id/edit', component: TaskFormComponent }
    ])
  ]
})
export class TasksModule {}
