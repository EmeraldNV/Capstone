import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { environment } from '../../../environments/environment';
import {
  AdminAuditLogResponse,
  AdminCreateUserRequest,
  AssignRolesRequest,
  AdminUserResponse,
  AdminUserUpdateRequest,
  RoleAssignmentResponse,
  UserResponse,
} from '../models/api.models';

@Injectable({
  providedIn: 'root',
})
export class AdminUserApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/admin/users`;

  listUsers(search?: string | null) {
    return this.http.get<AdminUserResponse[]>(this.baseUrl, {
      params: search ? { search } : undefined,
    });
  }

  getUser(userId: number) {
    return this.http.get<AdminUserResponse>(`${this.baseUrl}/${userId}`);
  }

  createUser(request: AdminCreateUserRequest) {
    return this.http.post<UserResponse>(this.baseUrl, request);
  }

  updateUser(userId: number, request: AdminUserUpdateRequest) {
    return this.http.put<AdminUserResponse>(`${this.baseUrl}/${userId}`, request);
  }

  deactivateUser(userId: number) {
    return this.http.delete<AdminUserResponse>(`${this.baseUrl}/${userId}`);
  }

  assignRoles(userId: number, request: AssignRolesRequest) {
    return this.http.post<RoleAssignmentResponse>(`${this.baseUrl}/${userId}/roles`, request);
  }

  listAuditLogs(entityName?: string | null) {
    return this.http.get<AdminAuditLogResponse[]>(`${this.baseUrl}/audit-logs`, {
      params: entityName ? { entityName } : undefined,
    });
  }
}
