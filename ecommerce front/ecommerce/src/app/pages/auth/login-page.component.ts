import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthApiService } from '../../core/services/auth-api.service';
import { SessionService } from '../../core/services/session.service';
import { mapAuthErrorMessage, summarizeHttpError } from '../../core/utils/http-errors';
import { ToastService } from '../../core/services/toast.service';

@Component({
  selector: 'app-login-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './login-page.component.html',
  styleUrl: './login-page.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LoginPageComponent {
  private readonly fb = inject(FormBuilder);
  private readonly authApi = inject(AuthApiService);
  private readonly session = inject(SessionService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);

  protected loading = false;
  protected successMessage = '';
  protected errorMessage = '';
  protected errorDetails: string[] = [];

  readonly form = this.fb.group({
    email: ['', [Validators.required, Validators.email, Validators.maxLength(255)]],
    password: ['', [Validators.required, Validators.minLength(1), Validators.maxLength(72)]],
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
      .login({ email: value.email ?? '', password: value.password ?? '' })
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: (response) => {
          this.session.applyAuthResponse(response);
          this.successMessage = `Accesso completato per ${response.user.email}.`;
          this.toast.success(`Benvenuto, ${response.user.email}.`, 'Login riuscito');
          void this.router.navigateByUrl(response.user.roles.includes('ADMIN') ? '/admin' : '/account/profile');
        },
        error: (error) => {
          const summary = summarizeHttpError(error, 'Login failed.');
          const toast = mapAuthErrorMessage(error);
          this.errorMessage = summary.message;
          this.errorDetails = summary.details;
          this.toast.error(toast.message, toast.title);
        },
      });
  }
}
