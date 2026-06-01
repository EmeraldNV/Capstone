import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { environment } from '../../../environments/environment';
import {
  AdminDashboardFilterOptionsResponse,
  AdminDashboardFilters,
  AdminDashboardResponse,
} from '../models/api.models';

@Injectable({
  providedIn: 'root',
})
export class AdminDashboardApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/admin/dashboard`;

  getFilterOptions() {
    return this.http.get<AdminDashboardFilterOptionsResponse>(`${this.baseUrl}/filters`);
  }

  getDashboard(filters: AdminDashboardFilters) {
    let params = new HttpParams();

    if (filters.from) {
      params = params.set('from', filters.from);
    }
    if (filters.to) {
      params = params.set('to', filters.to);
    }
    if (filters.categoryId != null) {
      params = params.set('categoryId', String(filters.categoryId));
    }
    if (filters.paymentMethodCode) {
      params = params.set('paymentMethodCode', filters.paymentMethodCode);
    }
    if (filters.orderStatus) {
      params = params.set('orderStatus', filters.orderStatus);
    }
    if (filters.paymentStatus) {
      params = params.set('paymentStatus', filters.paymentStatus);
    }

    return this.http.get<AdminDashboardResponse>(this.baseUrl, { params });
  }
}
