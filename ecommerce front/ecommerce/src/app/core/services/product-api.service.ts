import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { environment } from '../../../environments/environment';
import {
  ProductDetailResponse,
  ProductSummaryResponse,
  ProductUpsertRequest,
} from '../models/product.models';

@Injectable({
  providedIn: 'root',
})
export class ProductApiService {
  private readonly http = inject(HttpClient);
  private readonly publicBaseUrl = `${environment.apiBaseUrl}/products`;
  private readonly adminBaseUrl = `${environment.apiBaseUrl}/admin/products`;

  getPublicProductBySlug(slug: string) {
    return this.http.get<ProductDetailResponse>(`${this.publicBaseUrl}/${encodeURIComponent(slug)}`);
  }

  listPublicProducts(filters?: { category?: string | null }) {
    const params: Record<string, string> = {};
    if (filters?.category) {
      params['category'] = filters.category;
    }
    return this.http.get<ProductSummaryResponse[]>(this.publicBaseUrl, { params });
  }

  listAdminProducts() {
    return this.http.get<ProductSummaryResponse[]>(this.adminBaseUrl);
  }

  getAdminProduct(id: number) {
    return this.http.get<ProductDetailResponse>(`${this.adminBaseUrl}/${id}`);
  }

  createProduct(request: ProductUpsertRequest) {
    return this.http.post<ProductDetailResponse>(this.adminBaseUrl, request);
  }

  updateProduct(id: number, request: ProductUpsertRequest) {
    return this.http.put<ProductDetailResponse>(`${this.adminBaseUrl}/${id}`, request);
  }

  deleteProduct(id: number) {
    return this.http.delete<ProductDetailResponse>(`${this.adminBaseUrl}/${id}`);
  }

  uploadImages(id: number, files: Array<Blob & { name?: string }>) {
    const formData = new FormData();
    for (const file of files) {
      formData.append('files', file, file.name ?? 'image');
    }
    return this.http.post<ProductDetailResponse>(`${this.adminBaseUrl}/${id}/images`, formData);
  }

  deleteImage(productId: number, imageId: number) {
    return this.http.delete<ProductDetailResponse>(`${this.adminBaseUrl}/${productId}/images/${imageId}`);
  }

  setPrimaryImage(productId: number, imageId: number) {
    return this.http.post<ProductDetailResponse>(
      `${this.adminBaseUrl}/${productId}/images/${imageId}/primary`,
      {},
    );
  }

  reorderImages(productId: number, imageIds: number[]) {
    return this.http.put<ProductDetailResponse>(`${this.adminBaseUrl}/${productId}/images/order`, {
      imageIds,
    });
  }
}
