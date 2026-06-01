import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AuthApiService } from '../../core/services/auth-api.service';
import { summarizeHttpError } from '../../core/utils/http-errors';

@Component({
  selector: 'app-verify-email-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './verify-email-page.component.html',
  styleUrl: './verify-email-page.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class VerifyEmailPageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly authApi = inject(AuthApiService);

  protected loading = true;
  protected successMessage = '';
  protected errorMessage = '';
  protected errorDetails: string[] = [];

  ngOnInit(): void {
    const token = this.route.snapshot.queryParamMap.get('token');
    if (!token) {
      this.loading = false;
      this.errorMessage = 'Token di verifica mancante.';
      return;
    }

    this.authApi.verifyEmail(token).subscribe({
      next: (response) => {
        this.loading = false;
        this.successMessage = response.message;
      },
      error: (error) => {
        const summary = summarizeHttpError(error, 'Verification failed.');
        this.loading = false;
        this.errorMessage = summary.message;
        this.errorDetails = summary.details;
      },
    });
  }
}
