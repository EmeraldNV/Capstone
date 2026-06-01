import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthApiService } from '../../core/services/auth-api.service';
import { ToastService } from '../../core/services/toast.service';
import { summarizeHttpError } from '../../core/utils/http-errors';

@Component({
  selector: 'app-register-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './register-page.component.html',
  styleUrl: './register-page.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RegisterPageComponent {
  private readonly fb = inject(FormBuilder);
  private readonly authApi = inject(AuthApiService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);

  protected loading = false;
  protected successMessage = '';
  protected errorMessage = '';
  protected errorDetails: string[] = [];

  readonly form = this.fb.group({
    email: ['', [Validators.required, Validators.email, Validators.maxLength(255)]],
    password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(72)]],
    confirmPassword: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(72)]],
  });

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.toast.warning('Controlla i campi compilati prima di inviare il form.', 'Form incompleto');
      return;
    }

    this.loading = true;
    this.successMessage = '';
    this.errorMessage = '';
    this.errorDetails = [];

    const value = this.form.getRawValue();
    this.authApi
      .register({
        email: value.email ?? '',
        password: value.password ?? '',
        confirmPassword: value.confirmPassword ?? '',
      })
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: (response) => {
          this.successMessage = response.message;
          this.toast.success('Controlla la tua casella email per verificare l’account.', 'Registrazione completata');
          void this.router.navigateByUrl('/auth/login');
        },
        error: (error) => {
          const summary = summarizeHttpError(error, 'Registration failed.');
          this.errorMessage = summary.message;
          this.errorDetails = summary.details;
          this.toast.error(summary.message, 'Registrazione fallita');
        },
      });
  }
}
