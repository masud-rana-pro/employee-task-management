import { Component } from '@angular/core';

import { Toast, ToastService } from '../../core/services/toast.service';

/** Renders the notifications created through ToastService (top right corner of the screen). */
@Component({
  selector: 'app-toast-container',
  template: `
    <div class="toast-container position-fixed top-0 end-0 p-3" style="z-index: 1200">
      <ngb-toast
        *ngFor="let toast of toastService.toasts$ | async; trackBy: trackById"
        [ngClass]="'text-bg-' + toast.kind"
        [autohide]="true"
        [delay]="toast.delay"
        (hidden)="toastService.remove(toast.id)"
        role="alert"
      >
        <div class="d-flex align-items-start gap-2">
          <span class="flex-grow-1">{{ toast.message }}</span>
          <button type="button" class="btn-close btn-close-white ms-2" aria-label="Close"
                  (click)="toastService.remove(toast.id)"></button>
        </div>
      </ngb-toast>
    </div>
  `
})
export class ToastContainerComponent {
  constructor(public readonly toastService: ToastService) {}

  trackById(_index: number, toast: Toast): number {
    return toast.id;
  }
}
