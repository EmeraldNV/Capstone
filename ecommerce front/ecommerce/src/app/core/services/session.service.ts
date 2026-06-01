import { computed, Injectable, signal } from '@angular/core';
import { AuthResponse, AuthSession, UserResponse } from '../models/api.models';

const STORAGE_KEY = 'ecommerce.auth.session';

@Injectable({
  providedIn: 'root',
})
export class SessionService {
  private readonly sessionState = signal<AuthSession | null>(this.readStoredSession());

  readonly session = this.sessionState.asReadonly();
  readonly user = computed<UserResponse | null>(() => this.sessionState()?.user ?? null);
  readonly roles = computed<string[]>(() => this.sessionState()?.roles ?? []);
  readonly isAuthenticated = computed(() => this.sessionState() !== null);

  hasRole(role: string): boolean {
    return this.roles().includes(role.toUpperCase());
  }

  hasAnyRole(roles: string[]): boolean {
    const normalized = roles.map((role) => role.toUpperCase());
    return this.roles().some((role) => normalized.includes(role.toUpperCase()));
  }

  isAdmin(): boolean {
    return this.hasRole('ADMIN');
  }

  isStaff(): boolean {
    return this.hasAnyRole(['ADMIN', 'STAFF']);
  }

  applyAuthResponse(response: AuthResponse): void {
    const session: AuthSession = {
      accessToken: response.accessToken,
      tokenType: response.tokenType,
      expiresAt: Date.now() + response.expiresIn * 1000,
      user: response.user,
      roles: response.user.roles.map((role) => role.toUpperCase()),
    };

    this.sessionState.set(session);
    localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
  }

  clear(): void {
    this.sessionState.set(null);
    localStorage.removeItem(STORAGE_KEY);
  }

  logout(): void {
    this.clear();
  }

  getAccessToken(): string | null {
    return this.sessionState()?.accessToken ?? null;
  }

  private readStoredSession(): AuthSession | null {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) {
      return null;
    }

    try {
      const parsed = JSON.parse(raw) as AuthSession;
      if (!parsed.accessToken || !parsed.user || !Array.isArray(parsed.roles)) {
        return null;
      }
      if (typeof parsed.expiresAt !== 'number' || parsed.expiresAt <= Date.now()) {
        return null;
      }
      return {
        ...parsed,
        roles: parsed.roles.map((role) => role.toUpperCase()),
      };
    } catch {
      return null;
    }
  }
}
