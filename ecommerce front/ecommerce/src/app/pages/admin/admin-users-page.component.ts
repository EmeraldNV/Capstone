import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { FormsModule } from '@angular/forms';
import { AdminAuditLogResponse, AdminUserResponse } from '../../core/models/api.models';
import { AdminUserApiService } from '../../core/services/admin-user-api.service';
import { summarizeHttpError } from '../../core/utils/http-errors';

@Component({
  selector: 'app-admin-users-page',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './admin-users-page.component.html',
  styleUrl: './admin-users-page.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdminUsersPageComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(AdminUserApiService);

  protected loading = false;
  protected saving = false;
  protected errorMessage = '';
  protected errorDetails: string[] = [];
  protected successMessage = '';
  protected users: AdminUserResponse[] = [];
  protected auditLogs: AdminAuditLogResponse[] = [];
  protected selectedUser: AdminUserResponse | null = null;
  protected search = '';

  readonly createForm = this.fb.group({
    email: ['', [Validators.required, Validators.email, Validators.maxLength(255)]],
    password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(72)]],
    status: ['ACTIVE', [Validators.required]],
    emailVerified: [false],
    roleCodes: ['CUSTOMER', [Validators.required]],
  });

  readonly editForm = this.fb.group({
    email: ['', [Validators.required, Validators.email, Validators.maxLength(255)]],
    status: ['ACTIVE', [Validators.required]],
    emailVerified: [false],
    roleCodes: ['', [Validators.required]],
  });

  ngOnInit(): void {
    this.loadUsers();
    this.loadAuditLogs();
  }

  submitCreate(): void {
    if (this.createForm.invalid) {
      this.createForm.markAllAsTouched();
      return;
    }

    this.saving = true;
    this.clearMessages();
    const value = this.createForm.getRawValue();

    this.api.createUser({
      email: value.email ?? '',
      password: value.password ?? '',
      status: value.status ?? 'ACTIVE',
      emailVerified: Boolean(value.emailVerified),
      roleCodes: this.parseRoles(value.roleCodes ?? ''),
    }).subscribe({
      next: () => {
        this.saving = false;
        this.successMessage = 'Utente creato correttamente.';
        this.createForm.reset({
          email: '',
          password: '',
          status: 'ACTIVE',
          emailVerified: false,
          roleCodes: 'CUSTOMER',
        });
        this.loadUsers();
        this.loadAuditLogs();
      },
      error: (error) => this.handleError(error, 'Creazione utente fallita.'),
    });
  }

  submitUpdate(): void {
    if (!this.selectedUser) {
      this.errorMessage = 'Seleziona un utente da modificare.';
      return;
    }

    if (this.editForm.invalid) {
      this.editForm.markAllAsTouched();
      return;
    }

    this.saving = true;
    this.clearMessages();
    const value = this.editForm.getRawValue();

    this.api.updateUser(this.selectedUser.id, {
      email: value.email ?? '',
      status: value.status ?? 'ACTIVE',
      emailVerified: Boolean(value.emailVerified),
      roleCodes: this.parseRoles(value.roleCodes ?? ''),
    }).subscribe({
      next: (user) => {
        this.saving = false;
        this.selectedUser = user;
        this.successMessage = 'Utente aggiornato correttamente.';
        this.patchEditForm(user);
        this.loadUsers();
        this.loadAuditLogs();
      },
      error: (error) => this.handleError(error, 'Aggiornamento utente fallito.'),
    });
  }

  selectUser(user: AdminUserResponse): void {
    this.selectedUser = user;
    this.patchEditForm(user);
  }

  deactivateUser(user: AdminUserResponse): void {
    if (!window.confirm(`Disattivare ${user.email}?`)) {
      return;
    }

    this.saving = true;
    this.clearMessages();
    this.api.deactivateUser(user.id).subscribe({
      next: (updated) => {
        this.saving = false;
        if (this.selectedUser?.id === updated.id) {
          this.selectedUser = updated;
          this.patchEditForm(updated);
        }
        this.successMessage = 'Utente disattivato.';
        this.loadUsers();
        this.loadAuditLogs();
      },
      error: (error) => this.handleError(error, 'Disattivazione utente fallita.'),
    });
  }

  refresh(): void {
    this.loadUsers();
    this.loadAuditLogs();
  }

  searchUsers(): void {
    this.loadUsers();
  }

  formatDate(value: string | null | undefined): string {
    if (!value) {
      return '-';
    }
    return new Intl.DateTimeFormat('it-IT', {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    }).format(new Date(value));
  }

  private loadUsers(): void {
    this.loading = true;
    this.errorMessage = '';
    this.errorDetails = [];
    this.api.listUsers(this.search.trim() || null).subscribe({
      next: (users) => {
        this.loading = false;
        this.users = users;
        if (this.selectedUser) {
          const updated = users.find((item) => item.id === this.selectedUser?.id);
          if (updated) {
            this.selectedUser = updated;
            this.patchEditForm(updated);
          }
        }
      },
      error: (error) => this.handleError(error, 'Caricamento utenti fallito.'),
    });
  }

  private loadAuditLogs(): void {
    this.api.listAuditLogs('AppUser').subscribe({
      next: (logs) => {
        this.auditLogs = logs;
      },
      error: (error) => this.handleError(error, 'Caricamento cronologia fallito.'),
    });
  }

  private patchEditForm(user: AdminUserResponse): void {
    this.editForm.patchValue({
      email: user.email,
      status: user.status,
      emailVerified: user.emailVerified,
      roleCodes: user.roles.join(', '),
    });
  }

  private parseRoles(raw: string): string[] {
    return Array.from(
      new Set(
        raw
          .split(',')
          .map((value) => value.trim().toUpperCase())
          .filter((value) => value.length > 0),
      ),
    );
  }

  private handleError(error: unknown, fallback: string): void {
    this.saving = false;
    this.loading = false;
    const summary = summarizeHttpError(error, fallback);
    this.errorMessage = summary.message;
    this.errorDetails = summary.details;
  }

  private clearMessages(): void {
    this.errorMessage = '';
    this.errorDetails = [];
    this.successMessage = '';
  }
}
