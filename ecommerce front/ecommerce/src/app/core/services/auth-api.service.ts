import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { environment } from '../../../environments/environment';
import {
  AuthResponse,
  LoginRequest,
  RegisterRequest,
  RegistrationResponse,
  VerifyEmailResponse,
} from '../models/api.models';

@Injectable({
  providedIn: 'root',
})
export class AuthApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/auth`;

  login(request: LoginRequest) {
    return this.http.post<AuthResponse>(`${this.baseUrl}/login`, request);
  }

  register(request: RegisterRequest) {
    return this.http.post<RegistrationResponse>(`${this.baseUrl}/register`, request);
  }

  verifyEmail(token: string) {
    return this.http.get<VerifyEmailResponse>(`${this.baseUrl}/verify-email`, {
      params: { token },
    });
  }
}
