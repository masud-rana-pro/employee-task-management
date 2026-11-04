import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';

import { PriorityBadgeComponent, StatusBadgeComponent } from './components/badges.component';
import { ConfirmDialogComponent } from './components/confirm-dialog.component';
import { NavbarComponent } from './components/navbar.component';
import { ToastContainerComponent } from './components/toast-container.component';

/** Everything the feature modules have in common, so each of them only has to import this one module. */
@NgModule({
  declarations: [
    NavbarComponent,
    ToastContainerComponent,
    ConfirmDialogComponent,
    StatusBadgeComponent,
    PriorityBadgeComponent
  ],
  imports: [CommonModule, RouterModule, NgbModule],
  exports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    RouterModule,
    NgbModule,
    NavbarComponent,
    ToastContainerComponent,
    ConfirmDialogComponent,
    StatusBadgeComponent,
    PriorityBadgeComponent
  ]
})
export class SharedModule {}
