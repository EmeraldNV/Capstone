export interface StripeCheckoutItemRequest {
  productId: number;
  productVariantId?: number | null;
  quantity: number;
  variantLabel?: string | null;
}

export interface StripeCheckoutResponse {
  sessionId: string;
  url: string;
  status: string;
  amountTotal: number;
  currencyCode: string;
}

export interface StripeCheckoutStatusResponse {
  sessionId: string;
  status: string;
  checkoutType: string;
  orderNumber?: string | null;
  paymentIntentId?: string | null;
  customerEmail: string;
  amountTotal: number;
  currencyCode: string;
  message: string;
}

export interface PaymentStatusUpdate {
  eventType: string;
  sessionId: string;
  status: string;
  checkoutType: string;
  orderNumber?: string | null;
  paymentIntentId?: string | null;
  customerEmail: string;
  amountTotal: number;
  currencyCode: string;
  message: string;
  updatedAt: string;
  items?: string[];
}
