import { Injectable, signal } from '@angular/core';

export type ToastVariant = 'success' | 'error' | 'info' | 'warning';

export interface ToastItem {
  id: number;
  title: string;
  message: string;
  variant: ToastVariant;
}

@Injectable({
  providedIn: 'root',
})
export class ToastService {
  private readonly toastsState = signal<ToastItem[]>([]);
  private nextId = 1;

  readonly toasts = this.toastsState.asReadonly();

  show(message: string, options?: { title?: string; variant?: ToastVariant; durationMs?: number }): void {
    const id = this.nextId++;
    const toast: ToastItem = {
      id,
      title: options?.title ?? this.defaultTitle(options?.variant ?? 'info'),
      message,
      variant: options?.variant ?? 'info',
    };

    this.toastsState.update((current) => [...current, toast]);

    const durationMs = options?.durationMs ?? 4500;
    window.setTimeout(() => this.dismiss(id), durationMs);
  }

  success(message: string, title = 'Success'): void {
    this.show(message, { title, variant: 'success' });
  }

  error(message: string, title = 'Error'): void {
    this.show(message, { title, variant: 'error' });
  }

  info(message: string, title = 'Info'): void {
    this.show(message, { title, variant: 'info' });
  }

  warning(message: string, title = 'Warning'): void {
    this.show(message, { title, variant: 'warning' });
  }

  dismiss(id: number): void {
    this.toastsState.update((current) => current.filter((toast) => toast.id !== id));
  }

  clear(): void {
    this.toastsState.set([]);
  }

  private defaultTitle(variant: ToastVariant): string {
    switch (variant) {
      case 'success':
        return 'Success';
      case 'error':
        return 'Error';
      case 'warning':
        return 'Warning';
      default:
        return 'Info';
    }
  }
}