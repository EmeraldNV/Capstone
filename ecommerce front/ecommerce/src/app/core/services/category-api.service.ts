import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { catchError, defer, finalize, Observable, of, shareReplay, tap, timeout, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CategoryNavigationResponse } from '../models/category.models';

@Injectable({
  providedIn: 'root',
})
export class CategoryApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/categories`;
  private readonly storageKey = 'ecommerce.category-navigation.v1';
  private navigationCache: CategoryNavigationResponse[] | null = null;
  private navigationRequest$?: Observable<CategoryNavigationResponse[]>;

  listNavigationTree(forceRefresh = false): Observable<CategoryNavigationResponse[]> {
    if (!forceRefresh) {
      const cached = this.getCachedNavigationTree();
      if (cached.length) {
        return of(cached);
      }
      if (this.navigationCache) {
        return of(this.navigationCache);
      }
      if (this.navigationRequest$) {
        return this.navigationRequest$;
      }
    }

    const request$ = defer(() =>
      this.http.get<CategoryNavigationResponse[]>(`${this.baseUrl}/navigation`).pipe(
        timeout({ first: 8000 }),
        tap((categories) => {
          this.navigationCache = categories;
          this.saveCache(categories);
        }),
        catchError((error) => {
          const cached = this.getCachedNavigationTree();
          if (cached.length) {
            this.navigationCache = cached;
            return of(cached);
          }
          return throwError(() => error);
        }),
        finalize(() => {
          if (this.navigationRequest$ === request$) {
            this.navigationRequest$ = undefined;
          }
        }),
        shareReplay({ bufferSize: 1, refCount: false }),
      ),
    );

    this.navigationRequest$ = request$;
    return request$;
  }

  refreshNavigationTree(): Observable<CategoryNavigationResponse[]> {
    return this.listNavigationTree(true);
  }

  private getCachedNavigationTree(): CategoryNavigationResponse[] {
    if (this.navigationCache) {
      return this.navigationCache;
    }

    if (typeof window === 'undefined') {
      return [];
    }

    try {
      const raw = window.localStorage.getItem(this.storageKey);
      if (!raw) {
        return [];
      }
      const parsed = JSON.parse(raw) as CategoryNavigationResponse[];
      if (!Array.isArray(parsed)) {
        return [];
      }
      this.navigationCache = parsed;
      return parsed;
    } catch {
      return [];
    }
  }

  private saveCache(categories: CategoryNavigationResponse[]): void {
    if (typeof window === 'undefined') {
      return;
    }

    try {
      window.localStorage.setItem(this.storageKey, JSON.stringify(categories));
    } catch {
      // Ignore storage failures and keep the in-memory cache.
    }
  }
}
