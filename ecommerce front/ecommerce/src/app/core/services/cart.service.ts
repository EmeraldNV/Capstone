import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { CartItem } from '../models/cart.models';
import { ProductSummaryResponse } from '../models/product.models';

export type CartProductSource = Pick<
  ProductSummaryResponse,
  | 'id'
  | 'productCode'
  | 'slug'
  | 'name'
  | 'shortDescription'
  | 'listPrice'
  | 'salePrice'
  | 'effectivePrice'
  | 'currencyCode'
  | 'brandName'
  | 'categoryName'
  | 'categorySlug'
> & {
  thumbnailUrl?: string | null;
  images?: Array<{ imageUrl: string }> | null;
};

@Injectable({
  providedIn: 'root',
})
export class CartService {
  private readonly storageKey = 'ecommerce.cart.v1';
  private readonly itemsSubject = new BehaviorSubject<CartItem[]>(this.readCart());

  readonly items$ = this.itemsSubject.asObservable();
  readonly count$ = new BehaviorSubject<number>(this.getCount());

  get items(): CartItem[] {
    return this.itemsSubject.value;
  }

  addProduct(product: CartProductSource): void {
    const items = [...this.itemsSubject.value];
    const existing = items.find((item) => item.productId === product.id);

    if (existing) {
      existing.quantity += 1;
    } else {
      items.unshift({
        productId: product.id,
        productCode: product.productCode,
        slug: product.slug,
        name: product.name,
        price: product.effectivePrice,
        currencyCode: product.currencyCode,
        imageUrl: product.thumbnailUrl ?? product.images?.[0]?.imageUrl ?? null,
        shortDescription: product.shortDescription ?? null,
        brandName: product.brandName ?? null,
        categoryName: product.categoryName ?? null,
        categorySlug: product.categorySlug ?? null,
        quantity: 1,
      });
    }

    this.persist(items);
  }

  setQuantity(productId: number, quantity: number): void {
    const nextQuantity = Math.max(1, Math.floor(quantity || 1));
    const items = this.itemsSubject.value.map((item) =>
      item.productId === productId ? { ...item, quantity: nextQuantity } : item,
    );
    this.persist(items);
  }

  increase(productId: number): void {
    const item = this.itemsSubject.value.find((current) => current.productId === productId);
    if (!item) {
      return;
    }
    this.setQuantity(productId, item.quantity + 1);
  }

  decrease(productId: number): void {
    const item = this.itemsSubject.value.find((current) => current.productId === productId);
    if (!item) {
      return;
    }
    if (item.quantity <= 1) {
      this.remove(productId);
      return;
    }
    this.setQuantity(productId, item.quantity - 1);
  }

  remove(productId: number): void {
    const items = this.itemsSubject.value.filter((item) => item.productId !== productId);
    this.persist(items);
  }

  clear(): void {
    this.persist([]);
  }

  getTotal(): number {
    return this.itemsSubject.value.reduce((sum, item) => sum + item.price * item.quantity, 0);
  }

  getCount(): number {
    return this.itemsSubject.value.reduce((sum, item) => sum + item.quantity, 0);
  }

  private persist(items: CartItem[]): void {
    this.itemsSubject.next(items);
    this.count$.next(items.reduce((sum, item) => sum + item.quantity, 0));

    if (typeof window === 'undefined') {
      return;
    }

    try {
      window.localStorage.setItem(this.storageKey, JSON.stringify(items));
    } catch {
      // Ignore storage failures and keep in-memory state.
    }
  }

  private readCart(): CartItem[] {
    if (typeof window === 'undefined') {
      return [];
    }

    try {
      const raw = window.localStorage.getItem(this.storageKey);
      if (!raw) {
        return [];
      }
      const parsed = JSON.parse(raw) as CartItem[];
      return Array.isArray(parsed) ? parsed : [];
    } catch {
      return [];
    }
  }
}
