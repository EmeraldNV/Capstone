import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { catchError, defer, finalize, Observable, of, retry, shareReplay, tap, timeout, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { summarizeHttpError } from '../utils/http-errors';
import { ToastService } from './toast.service';
import { ProductDetailResponse, ProductSummaryResponse, ProductUpsertRequest } from '../models/product.models';
import { PageResponse, TagliaResponse } from '../models/taglia.models';

@Injectable({
  providedIn: 'root',
})
export class TagliaService {
  private readonly http = inject(HttpClient);
  private readonly toast = inject(ToastService);
  private readonly apiRoot = environment.apiBaseUrl.replace(/\/v1$/, '') || '/api';
  private readonly taglieUrl = `${this.apiRoot}/taglie`;
  private readonly prodottiUrl = `${this.apiRoot}/prodotti`;

  private taglieCache$?: Observable<TagliaResponse[]>;
  private cachedTaglie: TagliaResponse[] | null = null;

  getTaglie(forceRefresh = false): Observable<TagliaResponse[]> {
    if (!forceRefresh) {
      if (this.cachedTaglie) {
        return of(this.cachedTaglie);
      }
      if (this.taglieCache$) {
        return this.taglieCache$;
      }
    }

    const request$ = defer(() =>
      this.http.get<TagliaResponse[]>(this.taglieUrl).pipe(
        timeout({ first: 8000 }),
        retry({ count: 2, delay: 350 }),
        tap((taglie) => {
          this.cachedTaglie = taglie;
        }),
        catchError((error) => {
          const fallback = this.cachedTaglie;
          if (fallback && fallback.length) {
            return of(fallback);
          }

          const summary = summarizeHttpError(error, 'Caricamento taglie fallito.');
          this.notifyError(summary.message, summary.details);
          console.error('[TagliaService] getTaglie failed', error);
          return throwError(() => error);
        }),
        finalize(() => {
          if (this.taglieCache$ === request$ && !this.cachedTaglie) {
            this.taglieCache$ = undefined;
          }
        }),
        shareReplay({ bufferSize: 1, refCount: false }),
      ),
    );

    this.taglieCache$ = request$;
    return request$;
  }

  refreshTaglie(): Observable<TagliaResponse[]> {
    this.taglieCache$ = undefined;
    this.cachedTaglie = null;
    return this.getTaglie(true);
  }

  getProductsByTaglia(
    tagliaId: number,
    options?: { page?: number; size?: number },
  ): Observable<PageResponse<ProductSummaryResponse>> {
    const params = new HttpParams()
      .set('taglia', String(tagliaId))
      .set('page', String(options?.page ?? 0))
      .set('size', String(options?.size ?? 20));

    return defer(() =>
      this.http.get<PageResponse<ProductSummaryResponse>>(this.prodottiUrl, { params }).pipe(
        timeout({ first: 8000 }),
        retry({ count: 1, delay: 250 }),
        catchError((error) => {
          const summary = summarizeHttpError(error, 'Caricamento prodotti filtrati fallito.');
          this.notifyError(summary.message, summary.details);
          console.error('[TagliaService] getProductsByTaglia failed', error);
          return throwError(() => error);
        }),
        shareReplay({ bufferSize: 1, refCount: false }),
      ),
    );
  }

  createProduct(productData: ProductUpsertRequest): Observable<ProductDetailResponse> {
    return this.http.post<ProductDetailResponse>(this.prodottiUrl, productData).pipe(
      catchError((error) => this.handleWriteError(error, 'Creazione prodotto fallita.')),
    );
  }

  updateProduct(id: number, productData: ProductUpsertRequest): Observable<ProductDetailResponse> {
    return this.http.put<ProductDetailResponse>(`${this.prodottiUrl}/${id}`, productData).pipe(
      catchError((error) => this.handleWriteError(error, 'Aggiornamento prodotto fallito.')),
    );
  }

  private handleWriteError(error: unknown, fallbackMessage: string): Observable<never> {
    const summary = summarizeHttpError(error, fallbackMessage);
    this.notifyError(summary.message, summary.details);
    console.error('[TagliaService] write request failed', error);
    return throwError(() => error);
  }

  private notifyError(message: string, details: string[]): void {
    this.toast.error(message);
    for (const detail of details) {
      this.toast.warning(detail, 'Dettaglio');
    }
  }
}
