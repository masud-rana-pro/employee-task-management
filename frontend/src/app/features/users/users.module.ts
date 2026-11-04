import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { UserFormComponent } from './user-form.component';
import { UserListComponent } from './user-list.component';

@NgModule({
  declarations: [UserListComponent, UserFormComponent],
  imports: [SharedModule, RouterModule.forChild([{ path: '', component: UserListComponent }])]
})
export class UsersModule {}
