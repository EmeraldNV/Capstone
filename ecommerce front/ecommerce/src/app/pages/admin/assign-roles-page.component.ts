import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AdminUserApiService } from '../../core/services/admin-user-api.service';
import { summarizeHttpError } from '../../core/utils/http-errors';

@Component({
  selector: 'app-assign-roles-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './assign-roles-page.component.html',
  styleUrl: './assign-roles-page.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AssignRolesPageComponent {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(AdminUserApiService);

  protected loading = false;
  protected successMessage = '';
  protected errorMessage = '';
  protected errorDetails: string[] = [];
  protected resultRoles: string[] = [];

  readonly form = this.fb.group({
    userId: [null as number | null, [Validators.required, Validators.min(1)]],
    roleCodes: ['STAFF'],
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
    this.resultRoles = [];

    const value = this.form.getRawValue();
    const userId = Number(value.userId);

    this.api
      .assignRoles(userId, {
        roleCodes: this.parseCodes(value.roleCodes ?? ''),
      })
      .subscribe({
        next: (response) => {
          this.loading = false;
          this.successMessage = response.message;
          this.resultRoles = response.assignedRoles;
        },
        error: (error) => {
          const summary = summarizeHttpError(error, 'Assign roles failed.');
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
