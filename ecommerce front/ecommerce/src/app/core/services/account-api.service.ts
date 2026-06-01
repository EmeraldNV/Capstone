import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { environment } from '../../../environments/environment';
import { UpdateProfileRequest, UserProfileResponse } from '../models/api.models';

@Injectable({
  providedIn: 'root',
})
export class AccountApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/account`;

  getProfile() {
    return this.http.get<UserProfileResponse>(`${this.baseUrl}/me`);
  }

  updateProfile(request: UpdateProfileRequest) {
    return this.http.put<UserProfileResponse>(`${this.baseUrl}/me`, request);
  }
}
