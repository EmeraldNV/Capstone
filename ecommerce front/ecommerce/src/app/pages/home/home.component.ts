import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, DestroyRef, OnDestroy, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, finalize, of, timeout } from 'rxjs';
import { HomeCarouselImageResponse } from '../../core/models/home-carousel.models';
import { ProductSummaryResponse } from '../../core/models/product.models';
import { HomeCarouselApiService } from '../../core/services/home-carousel-api.service';
import { ProductApiService } from '../../core/services/product-api.service';
import { summarizeHttpError } from '../../core/utils/http-errors';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './home.component.html',
  styleUrl: './home.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HomeComponent implements OnInit, OnDestroy {
  private static readonly requestTimeoutMs = 6000;
  private readonly api = inject(ProductApiService);
  private readonly carouselApi = inject(HomeCarouselApiService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly cdr = inject(ChangeDetectorRef);

  protected loadingProducts = true;
  protected productError = '';
  protected latestProducts: ProductSummaryResponse[] = [];
  protected homeCarouselImages: HomeCarouselImageResponse[] = [];
  protected homeCarouselIndex = 0;

  private carouselTimer: ReturnType<typeof setInterval> | null = null;

  readonly featureCards = [
    {
      title: 'Nuovi arrivi',
      description: 'Lo shop pubblico mostra i prodotti con immagine, prezzo e accesso al dettaglio.',
    },
    {
      title: 'Layout scuro',
      description: 'Il tratto dominante resta quello del riferimento: sfondo nero, tipografia forte e contrasto alto.',
    },
  ];

  ngOnInit(): void {
    this.carouselApi.carousel$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((images) => {
      this.homeCarouselImages = images;
      if (this.homeCarouselIndex >= images.length) {
        this.homeCarouselIndex = 0;
      }
      this.restartCarouselTimer();
      this.cdr.markForCheck();
    });

    this.loadHomeCarousel();
    this.loadLatestProducts();
  }

  ngOnDestroy(): void {
    this.stopCarouselTimer();
  }

  productImage(product: ProductSummaryResponse): string {
    return product.thumbnailUrl ?? product.images[0]?.imageUrl ?? '';
  }

  homeCarouselTransform(): string {
    return `translateX(-${this.homeCarouselIndex * 100}%)`;
  }

  formatMoney(value: number, currencyCode: string): string {
    return new Intl.NumberFormat('it-IT', {
      style: 'currency',
      currency: currencyCode || 'EUR',
    }).format(value);
  }

  private loadLatestProducts(): void {
    this.loadingProducts = true;
    this.api
      .listPublicProducts()
      .pipe(
        timeout({ first: HomeComponent.requestTimeoutMs }),
        catchError((error) => {
          this.productError = summarizeHttpError(error, 'Caricamento prodotti fallito.').message;
          this.cdr.markForCheck();
          return of([] as ProductSummaryResponse[]);
        }),
        finalize(() => {
          this.loadingProducts = false;
          this.cdr.markForCheck();
        }),
      )
      .subscribe((products) => {
        this.latestProducts = [...products]
          .sort((left, right) => {
            const leftTime = new Date(left.createdAt).getTime();
            const rightTime = new Date(right.createdAt).getTime();
            return rightTime - leftTime;
          })
          .slice(0, 4);
        this.cdr.markForCheck();
      });
  }

  private loadHomeCarousel(): void {
    this.carouselApi.loadPublicCarousel().subscribe({
      next: () => {
        this.cdr.markForCheck();
      },
      error: () => {
        this.homeCarouselImages = [];
        this.homeCarouselIndex = 0;
        this.stopCarouselTimer();
        this.cdr.markForCheck();
      },
    });
  }

  private restartCarouselTimer(): void {
    this.stopCarouselTimer();
    if (this.homeCarouselImages.length <= 1) {
      return;
    }

    this.carouselTimer = setInterval(() => {
      if (!this.homeCarouselImages.length) {
        return;
      }
      this.homeCarouselIndex = (this.homeCarouselIndex + 1) % this.homeCarouselImages.length;
      this.cdr.markForCheck();
    }, 3500);
  }

  private stopCarouselTimer(): void {
    if (this.carouselTimer) {
      clearInterval(this.carouselTimer);
      this.carouselTimer = null;
    }
  }
}
