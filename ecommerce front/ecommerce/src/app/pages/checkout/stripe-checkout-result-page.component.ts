import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, DestroyRef, OnDestroy, OnInit, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CartService } from '../../core/services/cart.service';
import { PaymentApiService } from '../../core/services/payment-api.service';
import { PaymentWebSocketService } from '../../core/services/payment-websocket.service';
import { summarizeHttpError } from '../../core/utils/http-errors';

@Component({
  selector: 'app-stripe-checkout-result-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './stripe-checkout-result-page.component.html',
  styleUrl: './stripe-checkout-result-page.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StripeCheckoutResultPageComponent implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly api = inject(PaymentApiService);
  private readonly cart = inject(CartService);
  private readonly paymentSocket = inject(PaymentWebSocketService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly cdr = inject(ChangeDetectorRef);

  protected loading = true;
  protected result = 'success';
  protected message = '';
  protected errorMessage = '';
  protected orderNumber = '';
  protected sessionStatus = '';
  protected realtimeConnected = false;
  protected realtimeMessage = '';

  ngOnInit(): void {
    this.route.paramMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((params) => {
      this.result = params.get('result') ?? 'success';
      const sessionId = this.route.snapshot.queryParamMap.get('session_id') ?? '';

      if (this.result === 'success') {
        this.paymentSocket.connect(sessionId);
        this.paymentSocket.connected$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((connected) => {
          this.realtimeConnected = connected;
          this.cdr.markForCheck();
        });
        this.paymentSocket.updates$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((update) => {
          if (update.sessionId !== sessionId) {
            return;
          }
          this.realtimeMessage = update.message;
          this.applyStatusUpdate(update.status, update.orderNumber ?? '', update.message);
        });
        this.pollSessionStatus(sessionId, 0);
      } else {
        this.loading = false;
        this.message = 'Payment canceled. You can go back to the shop or try again from the cart.';
        this.cdr.markForCheck();
      }
    });
  }

  ngOnDestroy(): void {
    this.paymentSocket.disconnect();
  }

  private pollSessionStatus(sessionId: string, attempt: number): void {
    if (!sessionId) {
      this.loading = false;
      this.message = 'Stripe session was not found in the redirect.';
      this.cdr.markForCheck();
      return;
    }

    if (this.isTerminalStatus(this.sessionStatus)) {
      return;
    }

    this.api.getCheckoutStatus(sessionId).subscribe({
      next: (status) => {
        this.sessionStatus = status.status;
        this.message = status.message;
        this.orderNumber = status.orderNumber ?? this.orderNumber;

        if (this.isTerminalStatus(status.status)) {
          this.applyStatusUpdate(status.status, status.orderNumber ?? '', status.message);
          return;
        }

        if (status.status === 'PENDING' && attempt < 10) {
          setTimeout(() => this.pollSessionStatus(sessionId, attempt + 1), 1000);
          return;
        }

        this.loading = false;
        this.message = status.message;
        this.cdr.markForCheck();
      },
      error: (error) => {
        if (attempt < 6) {
          setTimeout(() => this.pollSessionStatus(sessionId, attempt + 1), 1200);
          return;
        }
        this.loading = false;
        const summary = summarizeHttpError(error, 'Payment status unavailable.');
        this.errorMessage = summary.message;
        this.cdr.markForCheck();
      },
    });
  }

  private applyStatusUpdate(status: string, orderNumber: string, message: string): void {
    this.sessionStatus = status;
    this.orderNumber = orderNumber || this.orderNumber;
    this.message = message;
    if (this.isTerminalStatus(status)) {
      this.loading = false;
      if (status === 'COMPLETED') {
        this.cart.clear();
      }
    }
    this.cdr.markForCheck();
  }

  private isTerminalStatus(status: string): boolean {
    return ['COMPLETED', 'CANCELED', 'EXPIRED', 'FAILED'].includes(status);
  }
}
