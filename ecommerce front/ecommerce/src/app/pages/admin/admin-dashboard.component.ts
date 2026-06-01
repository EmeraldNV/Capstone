import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import {
  AdminDashboardFilterOptionsResponse,
  AdminDashboardResponse,
  AdminFilterOptionResponse,
} from '../../core/models/api.models';
import { AdminDashboardApiService } from '../../core/services/admin-dashboard-api.service';
import { summarizeHttpError } from '../../core/utils/http-errors';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './admin-dashboard.component.html',
  styleUrl: './admin-dashboard.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdminDashboardComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(AdminDashboardApiService);

  protected loading = false;
  protected errorMessage = '';
  protected errorDetails: string[] = [];
  protected filterOptions: AdminDashboardFilterOptionsResponse | null = null;
  protected dashboard: AdminDashboardResponse | null = null;

  readonly form = this.fb.group({
    from: [''],
    to: [''],
    categoryId: [''],
    paymentMethodCode: [''],
    orderStatus: [''],
    paymentStatus: [''],
  });

  ngOnInit(): void {
    this.loadFilterOptions();
    this.loadDashboard();
  }

  submit(): void {
    this.loadDashboard();
  }

  reset(): void {
    const end = new Date();
    const start = new Date();
    start.setDate(end.getDate() - 30);

    this.form.reset({
      from: this.toInputDate(start),
      to: this.toInputDate(end),
      categoryId: '',
      paymentMethodCode: '',
      orderStatus: '',
      paymentStatus: '',
    });
    this.loadDashboard();
  }

  protected formatMoney(value: number | null | undefined, currencyCode = 'EUR'): string {
    if (value == null) {
      return '-';
    }
    return new Intl.NumberFormat('it-IT', { style: 'currency', currency: currencyCode }).format(value);
  }

  protected formatDate(value: string | null | undefined): string {
    if (!value) {
      return '-';
    }
    return new Intl.DateTimeFormat('it-IT', { day: '2-digit', month: 'short', year: 'numeric' }).format(
      new Date(value),
    );
  }

  protected formatFilterLabel(option?: AdminFilterOptionResponse | null): string {
    return option ? option.label : '-';
  }

  protected trendPath(): string {
    const points = this.trendPoints();
    if (points.length < 2) {
      return '';
    }
    return points
      .map((point, index) => `${index === 0 ? 'M' : 'L'} ${point.x.toFixed(1)} ${point.y.toFixed(1)}`)
      .join(' ');
  }

  protected trendPoints(): Array<{ x: number; y: number; date: string; revenue: number }> {
    const trend = this.dashboard?.salesTrend ?? [];
    if (!trend.length) {
      return [];
    }

    const width = 640;
    const height = 220;
    const padding = 24;
    const innerWidth = width - padding * 2;
    const innerHeight = height - padding * 2;
    const maxRevenue = Math.max(...trend.map((item) => item.revenue), 1);

    return trend.map((item, index) => ({
      x: trend.length === 1 ? width / 2 : padding + (innerWidth * index) / (trend.length - 1),
      y: padding + innerHeight - (innerHeight * item.revenue) / maxRevenue,
      date: item.date,
      revenue: item.revenue,
    }));
  }

  protected categoryBars(): Array<{ label: string; revenue: number; percent: number }> {
    const categories = this.dashboard?.categoryBreakdown ?? [];
    const maxRevenue = Math.max(...categories.map((item) => item.revenue), 1);
    return categories.map((item) => ({
      label: item.categoryName,
      revenue: item.revenue,
      percent: Math.round((item.revenue / maxRevenue) * 100),
    }));
  }

  protected activeCategoryLabel(): string {
    const selected = this.form.get('categoryId')?.value || '';
    if (!selected) {
      return 'Tutte';
    }
    const match = this.filterOptions?.categories.find((item) => item.value === selected);
    return match?.label ?? selected;
  }

  private loadFilterOptions(): void {
    this.api.getFilterOptions().subscribe({
      next: (options) => {
        this.filterOptions = options;
        const today = new Date();
        const start = new Date();
        start.setDate(today.getDate() - 30);
        this.form.patchValue({
          from: this.toInputDate(start),
          to: this.toInputDate(today),
        });
      },
      error: (error) => {
        const summary = summarizeHttpError(error, 'Caricamento filtri dashboard fallito.');
        this.errorMessage = summary.message;
        this.errorDetails = summary.details;
      },
    });
  }

  private loadDashboard(): void {
    this.loading = true;
    this.errorMessage = '';
    this.errorDetails = [];

    const value = this.form.getRawValue();
    this.api
      .getDashboard({
        from: value.from || null,
        to: value.to || null,
        categoryId: value.categoryId ? Number(value.categoryId) : null,
        paymentMethodCode: value.paymentMethodCode || null,
        orderStatus: value.orderStatus || null,
        paymentStatus: value.paymentStatus || null,
      })
      .subscribe({
        next: (dashboard) => {
          this.loading = false;
          this.dashboard = dashboard;
        },
        error: (error) => {
          this.loading = false;
          const summary = summarizeHttpError(error, 'Caricamento statistiche fallito.');
          this.errorMessage = summary.message;
          this.errorDetails = summary.details;
        },
      });
  }

  private toInputDate(date: Date): string {
    const year = date.getFullYear();
    const month = `${date.getMonth() + 1}`.padStart(2, '0');
    const day = `${date.getDate()}`.padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
}
