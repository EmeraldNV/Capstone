import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AdminUserApiService } from '../../core/services/admin-user-api.service';
import { SessionService } from '../../core/services/session.service';
import { summarizeHttpError } from '../../core/utils/http-errors';

@Component({
  selector: 'app-create-user-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './create-user-page.component.html',
  styleUrl: './create-user-page.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CreateUserPageComponent {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(AdminUserApiService);
  protected readonly session = inject(SessionService);

  protected loading = false;
  protected successMessage = '';
  protected errorMessage = '';
  protected errorDetails: string[] = [];

  readonly statuses = ['ACTIVE', 'INACTIVE', 'LOCKED', 'PENDING'];

  readonly form = this.fb.group({
    email: ['', [Validators.required, Validators.email, Validators.maxLength(255)]],
    password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(72)]],
    status: ['ACTIVE'],
    emailVerified: [false],
    roleCodes: ['CUSTOMER'],
  });

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading = true;
    this.successMessage = '';
    this.errorMessage = '';
    this.errorDetails = [];

    const value = this.form.getRawValue();
    this.api
      .createUser({
        email: value.email ?? '',
        password: value.password ?? '',
        status: value.status ?? 'ACTIVE',
        emailVerified: Boolean(value.emailVerified),
        roleCodes: this.parseCodes(value.roleCodes ?? ''),
      })
      .subscribe({
        next: (response) => {
          this.loading = false;
          this.successMessage = `Utente creato: ${response.email}.`;
          this.form.patchValue({
            email: '',
            password: '',
            status: 'ACTIVE',
            emailVerified: false,
            roleCodes: 'CUSTOMER',
          });
          this.form.markAsPristine();
        },
        error: (error) => {
          const summary = summarizeHttpError(error, 'Create user failed.');
          this.loading = false;
          this.errorMessage = summary.message;
          this.errorDetails = summary.details;
        },
      });
  }

  private parseCodes(raw: string): string[] {
    return Array.from(
      new Set(
        raw
          .split(',')
          .map((code) => code.trim().toUpperCase())
          .filter((code) => code.length > 0),
      ),
    );
  }
}
