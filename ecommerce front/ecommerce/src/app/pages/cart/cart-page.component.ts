import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CartItem } from '../../core/models/cart.models';
import { StripeCheckoutItemRequest } from '../../core/models/payment.models';
import { CartService } from '../../core/services/cart.service';
import { PaymentApiService } from '../../core/services/payment-api.service';
import { SessionService } from '../../core/services/session.service';
import { summarizeHttpError } from '../../core/utils/http-errors';

@Component({
  selector: 'app-cart-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './cart-page.component.html',
  styleUrl: './cart-page.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CartPageComponent {
  protected readonly cart = inject(CartService);
  private readonly paymentApi = inject(PaymentApiService);
  protected readonly session = inject(SessionService);
  private readonly cdr = inject(ChangeDetectorRef);
  protected checkoutLoading = false;
  protected checkoutError = '';

  formatMoney(value: number, currencyCode: string): string {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: currencyCode || 'EUR',
    }).format(value);
  }

  lineTotal(item: CartItem): number {
    return item.price * item.quantity;
  }

  currencyCode(): string {
    return this.cart.items[0]?.currencyCode ?? 'EUR';
  }

  checkout(): void {
    const email = this.session.user()?.email ?? '';
    if (!email) {
      this.checkoutError = 'You must sign in with an account before checkout.';
      this.cdr.markForCheck();
      return;
    }

    const items: StripeCheckoutItemRequest[] = this.cart.items.map((item) => ({
      productId: item.productId,
      productVariantId: null,
      quantity: item.quantity,
      variantLabel: null,
    }));

    this.checkoutLoading = true;
    this.checkoutError = '';
    this.cdr.markForCheck();

    this.paymentApi.createCartCheckout(email, items).subscribe({
      next: (response) => {
        window.location.assign(response.url);
      },
      error: (error) => {
        this.checkoutLoading = false;
        const summary = summarizeHttpError(error, 'Stripe checkout creation failed.');
        this.checkoutError = summary.message;
        this.cdr.markForCheck();
      },
    });
  }
}
