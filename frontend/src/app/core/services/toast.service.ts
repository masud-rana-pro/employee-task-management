import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

export type ToastKind = 'success' | 'danger' | 'warning' | 'info';

export interface Toast {
  id: number;
  kind: ToastKind;
  message: string;
  delay: number;
}

/** Tiny notification centre. Any class can call toast.success(...) / toast.error(...). */
@Injectable({ providedIn: 'root' })
export class ToastService {
  private nextId = 1;
  private readonly toastsSubject = new BehaviorSubject<Toast[]>([]);
  readonly toasts$ = this.toastsSubject.asObservable();

  success(message: string): void {
    this.show('success', message, 3500);
  }

  error(message: string): void {
    this.show('danger', message, 6000);
  }

  warning(message: string): void {
    this.show('warning', message, 5000);
  }

  info(message: string): void {
    this.show('info', message, 4000);
  }

  remove(id: number): void {
    this.toastsSubject.next(this.toastsSubject.value.filter(t => t.id !== id));
  }

  private show(kind: ToastKind, message: string, delay: number): void {
    const toast: Toast = { id: this.nextId++, kind, message, delay };
    // keep at most 4 toasts on screen
    this.toastsSubject.next([...this.toastsSubject.value, toast].slice(-4));
  }
}
