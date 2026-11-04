import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { MyTasksComponent } from './my-tasks.component';

@NgModule({
  declarations: [MyTasksComponent],
  imports: [SharedModule, RouterModule.forChild([{ path: '', component: MyTasksComponent }])]
})
export class MyTasksModule {}
