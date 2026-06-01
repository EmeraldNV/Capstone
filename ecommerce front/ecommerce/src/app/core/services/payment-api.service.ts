import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { environment } from '../../../environments/environment';
import {
  StripeCheckoutItemRequest,
  StripeCheckoutResponse,
  StripeCheckoutStatusResponse,
} from '../models/payment.models';

@Injectable({
  providedIn: 'root',
})
export class PaymentApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/payments/stripe`;

  createCartCheckout(customerEmail: string, items: StripeCheckoutItemRequest[]) {
    return this.http.post<StripeCheckoutResponse>(`${this.baseUrl}/cart`, {
      customerEmail,
      items,
    });
  }

  createQuickBuy(customerEmail: string, item: StripeCheckoutItemRequest) {
    return this.http.post<StripeCheckoutResponse>(`${this.baseUrl}/quick-buy`, {
      customerEmail,
      ...item,
    });
  }

  getCheckoutStatus(sessionId: string) {
    return this.http.get<StripeCheckoutStatusResponse>(`${this.baseUrl}/sessions/${encodeURIComponent(sessionId)}`);
  }
}
