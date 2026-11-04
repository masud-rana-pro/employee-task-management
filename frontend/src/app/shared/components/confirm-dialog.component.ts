import { Component, Injectable, Input } from '@angular/core';
import { NgbActiveModal, NgbModal } from '@ng-bootstrap/ng-bootstrap';

/** The modal window itself. Use ConfirmService.confirm(...) instead of opening it directly. */
@Component({
  selector: 'app-confirm-dialog',
  template: `
    <div class="modal-header">
      <h5 class="modal-title">{{ title }}</h5>
      <button type="button" class="btn-close" aria-label="Close" (click)="modal.dismiss()"></button>
    </div>
    <div class="modal-body">
      <p class="mb-0">{{ message }}</p>
    </div>
    <div class="modal-footer">
      <button type="button" class="btn btn-outline-secondary" (click)="modal.dismiss()">Cancel</button>
      <button type="button" class="btn btn-danger" (click)="modal.close(true)">{{ confirmLabel }}</button>
    </div>
  `
})
export class ConfirmDialogComponent {
  @Input() title = 'Please confirm';
  @Input() message = '';
  @Input() confirmLabel = 'Confirm';

  constructor(public readonly modal: NgbActiveModal) {}
}

/** Asks the user "are you sure?" and resolves to true (confirmed) or false (cancelled / closed). */
@Injectable({ providedIn: 'root' })
export class ConfirmService {
  constructor(private readonly modalService: NgbModal) {}

  confirm(message: string, title = 'Please confirm', confirmLabel = 'Delete'): Promise<boolean> {
    const ref = this.modalService.open(ConfirmDialogComponent, { centered: true });
    ref.componentInstance.title = title;
    ref.componentInstance.message = message;
    ref.componentInstance.confirmLabel = confirmLabel;
    return ref.result.then(() => true, () => false);
  }
}
