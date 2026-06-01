import { Injectable, inject } from '@angular/core';
import { BehaviorSubject, catchError, finalize, of, timeout } from 'rxjs';
import { ProductSummaryResponse } from '../models/product.models';
import { ProductApiService } from './product-api.service';
import { summarizeHttpError } from '../utils/http-errors';

export interface ShopCatalogState {
  products: ProductSummaryResponse[];
  loading: boolean;
  error: string;
  categorySlug: string | null;
}

@Injectable({
  providedIn: 'root',
})
export class ShopCatalogService {
  private static readonly requestTimeoutMs = 6000;

  private readonly productApi = inject(ProductApiService);
  private readonly stateSubject = new BehaviorSubject<ShopCatalogState>({
    products: [],
    loading: false,
    error: '',
    categorySlug: null,
  });
  private currentRequestKey = '';

  readonly state$ = this.stateSubject.asObservable();

  refresh(categorySlug: string | null = null): void {
    const requestKey = '__all__';
    if (this.currentRequestKey === requestKey && this.stateSubject.value.loading) {
      return;
    }

    this.currentRequestKey = requestKey;
    this.stateSubject.next({
      ...this.stateSubject.value,
      loading: true,
      error: '',
      categorySlug,
    });

    this.productApi
      .listPublicProducts()
      .pipe(
        timeout({ first: ShopCatalogService.requestTimeoutMs }),
        catchError((error) => {
          const message = summarizeHttpError(error, 'Caricamento shop fallito.').message;
          this.stateSubject.next({
            products: this.stateSubject.value.products,
            loading: false,
            error: message,
            categorySlug,
          });
          return of(this.stateSubject.value.products);
        }),
        finalize(() => {
          const current = this.stateSubject.value;
          if (current.loading) {
            this.stateSubject.next({
              ...current,
              loading: false,
            });
          }
        }),
      )
      .subscribe({
        next: (products) => {
          this.stateSubject.next({
            products,
            loading: false,
            error: '',
            categorySlug,
          });
        },
      });
  }
}
