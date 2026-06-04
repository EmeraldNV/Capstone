import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AccountApiService } from '../../core/services/account-api.service';
import { SessionService } from '../../core/services/session.service';
import { summarizeHttpError } from '../../core/utils/http-errors';

@Component({
  selector: 'app-profile-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './profile-page.component.html',
  styleUrl: './profile-page.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProfilePageComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly accountApi = inject(AccountApiService);
  protected readonly session = inject(SessionService);

  protected loading = true;
  protected saving = false;
  protected successMessage = '';
  protected errorMessage = '';
  protected errorDetails: string[] = [];

  readonly form = this.fb.group({
    firstName: ['', [Validators.required, Validators.maxLength(100)]],
    lastName: ['', [Validators.required, Validators.maxLength(100)]],
    companyName: ['', [Validators.maxLength(150)]],
    taxCode: ['', [Validators.maxLength(32)]],
    vatNumber: ['', [Validators.maxLength(32)]],
    birthDate: [''],
    phone: ['', [Validators.maxLength(30)]],
    marketingConsent: [false],
    address: this.fb.group({
      label: ['', [Validators.required, Validators.maxLength(100)]],
      recipientName: ['', [Validators.required, Validators.maxLength(150)]],
      companyName: ['', [Validators.maxLength(150)]],
      phone: ['', [Validators.maxLength(30)]],
      line1: ['', [Validators.required, Validators.maxLength(200)]],
      line2: ['', [Validators.maxLength(200)]],
      city: ['', [Validators.required, Validators.maxLength(100)]],
      stateRegion: ['', [Validators.maxLength(100)]],
      postalCode: ['', [Validators.required, Validators.maxLength(20)]],
      countryCode: ['IT', [Validators.required, Validators.pattern(/^[A-Z]{2}$/)]],
    }),
  });

  ngOnInit(): void {
    this.accountApi.getProfile().subscribe({
      next: (profile) => {
        this.loading = false;
        this.form.patchValue({
          firstName: profile.firstName ?? '',
          lastName: profile.lastName ?? '',
          companyName: profile.companyName ?? '',
          taxCode: profile.taxCode ?? '',
          vatNumber: profile.vatNumber ?? '',
          birthDate: profile.birthDate ?? '',
          phone: profile.phone ?? '',
          marketingConsent: profile.marketingConsent,
          address: {
            label: profile.address?.label ?? 'Primary',
            recipientName: profile.address?.recipientName ?? `${profile.firstName ?? ''} ${profile.lastName ?? ''}`.trim(),
            companyName: profile.address?.companyName ?? profile.companyName ?? '',
            phone: profile.address?.phone ?? profile.phone ?? '',
            line1: profile.address?.line1 ?? '',
            line2: profile.address?.line2 ?? '',
            city: profile.address?.city ?? '',
            stateRegion: profile.address?.stateRegion ?? '',
            postalCode: profile.address?.postalCode ?? '',
            countryCode: profile.address?.countryCode ?? 'IT',
          },
        });
      },
      error: (error) => {
        const summary = summarizeHttpError(error, 'Unable to load profile.');
        this.loading = false;
        this.errorMessage = summary.message;
        this.errorDetails = summary.details;
      },
    });
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.saving = true;
    this.successMessage = '';
    this.errorMessage = '';
    this.errorDetails = [];

    const value = this.form.getRawValue();
    this.accountApi
      .updateProfile({
        firstName: value.firstName ?? '',
        lastName: value.lastName ?? '',
        companyName: value.companyName ?? '',
        taxCode: value.taxCode ?? '',
        vatNumber: value.vatNumber ?? '',
        birthDate: value.birthDate ?? '',
        phone: value.phone ?? '',
        marketingConsent: Boolean(value.marketingConsent),
        address: {
          label: value.address?.label ?? '',
          recipientName: value.address?.recipientName ?? '',
          companyName: value.address?.companyName ?? '',
          phone: value.address?.phone ?? '',
          line1: value.address?.line1 ?? '',
          line2: value.address?.line2 ?? '',
          city: value.address?.city ?? '',
          stateRegion: value.address?.stateRegion ?? '',
          postalCode: value.address?.postalCode ?? '',
          countryCode: (value.address?.countryCode ?? '').toUpperCase(),
        },
      })
      .subscribe({
        next: (profile) => {
          this.saving = false;
          this.successMessage = `Profile updated for ${profile.email}.`;
        },
        error: (error) => {
          const summary = summarizeHttpError(error, 'Profile update failed.');
          this.saving = false;
          this.errorMessage = summary.message;
          this.errorDetails = summary.details;
        },
      });
  }
}
