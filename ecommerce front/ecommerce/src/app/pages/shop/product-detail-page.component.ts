import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, DestroyRef, HostListener, OnInit, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ProductDetailResponse, ProductImageResponse } from '../../core/models/product.models';
import { CartService } from '../../core/services/cart.service';
import { PaymentApiService } from '../../core/services/payment-api.service';
import { ProductApiService } from '../../core/services/product-api.service';
import { SessionService } from '../../core/services/session.service';
import { summarizeHttpError } from '../../core/utils/http-errors';

@Component({
  selector: 'app-product-detail-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './product-detail-page.component.html',
  styleUrl: './product-detail-page.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProductDetailPageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly api = inject(ProductApiService);
  private readonly cart = inject(CartService);
  private readonly paymentApi = inject(PaymentApiService);
  protected readonly session = inject(SessionService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly cdr = inject(ChangeDetectorRef);

  protected loading = true;
  protected errorMessage = '';
  protected product: ProductDetailResponse | null = null;
  protected selectedImageUrl = '';
  protected zoomedImageUrl = '';
  protected quickBuyLoading = false;
  protected quickBuyError = '';
  protected readonly carouselIndex = new Map<number, number>();
  private readonly touchStartX = new Map<number, number>();

  ngOnInit(): void {
    this.route.paramMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((params) => {
      const slug = params.get('slug');
      if (!slug) {
        this.loading = false;
        this.product = null;
        this.errorMessage = 'Slug prodotto mancante.';
        this.cdr.markForCheck();
        return;
      }
      this.loading = true;
      this.errorMessage = '';
      this.product = null;
      this.selectedImageUrl = '';
      this.zoomedImageUrl = '';
      this.cdr.markForCheck();
      this.api.getPublicProductBySlug(slug).subscribe({
        next: (product) => {
          this.loading = false;
          this.product = product;
          this.selectedImageUrl = this.currentCarouselImage(product)?.imageUrl ?? '';
          this.zoomedImageUrl = '';
          this.cdr.markForCheck();
        },
        error: (error) => {
          this.loading = false;
          this.errorMessage = summarizeHttpError(error, 'Caricamento dettaglio prodotto fallito.').message;
          this.cdr.markForCheck();
        },
      });
    });
  }

  selectImage(url: string): void {
    this.selectedImageUrl = url;
  }

  carouselImages(): ProductImageResponse[] {
    if (!this.product?.images?.length) {
      return [];
    }

    return [...this.product.images].sort((left, right) => {
      if (left.primary !== right.primary) {
        return left.primary ? -1 : 1;
      }
      return left.sortOrder - right.sortOrder;
    });
  }

  currentCarouselIndex(): number {
    const product = this.product;
    if (!product) {
      return 0;
    }

    const images = this.carouselImages();
    if (!images.length) {
      return 0;
    }

    const current = this.carouselIndex.get(product.id) ?? 0;
    return current >= images.length ? 0 : current;
  }

  currentCarouselImage(product?: ProductDetailResponse | null): ProductImageResponse | null {
    const currentProduct = product ?? this.product;
    if (!currentProduct) {
      return null;
    }

    const images = this.carouselImages();
    if (!images.length) {
      return null;
    }

    return images[this.currentCarouselIndex()] ?? images[0];
  }

  hasMultipleImages(): boolean {
    return this.carouselImages().length > 1;
  }

  carouselCountLabel(): string {
    const images = this.carouselImages();
    if (!images.length) {
      return '0 / 0';
    }
    return `${this.currentCarouselIndex() + 1} / ${images.length}`;
  }

  carouselTrackTransform(product?: ProductDetailResponse | null): string {
    const currentProduct = product ?? this.product;
    if (!currentProduct) {
      return 'translateX(0%)';
    }

    return `translateX(-${this.currentCarouselIndex() * 100}%)`;
  }

  nextCarouselImage(): void {
    const product = this.product;
    const images = this.carouselImages();
    if (!product || images.length <= 1) {
      return;
    }
    const current = this.currentCarouselIndex();
    this.carouselIndex.set(product.id, (current + 1) % images.length);
    this.selectedImageUrl = images[(current + 1) % images.length]?.imageUrl ?? '';
    this.cdr.markForCheck();
  }

  previousCarouselImage(): void {
    const product = this.product;
    const images = this.carouselImages();
    if (!product || images.length <= 1) {
      return;
    }
    const current = this.currentCarouselIndex();
    const nextIndex = (current - 1 + images.length) % images.length;
    this.carouselIndex.set(product.id, nextIndex);
    this.selectedImageUrl = images[nextIndex]?.imageUrl ?? '';
    this.cdr.markForCheck();
  }

  goToCarouselImage(index: number): void {
    const product = this.product;
    const images = this.carouselImages();
    if (!product || !images.length) {
      return;
    }
    const nextIndex = Math.max(0, Math.min(index, images.length - 1));
    this.carouselIndex.set(product.id, nextIndex);
    this.selectedImageUrl = images[nextIndex]?.imageUrl ?? '';
    this.cdr.markForCheck();
  }

  carouselSlideLabel(): string {
    return this.carouselCountLabel();
  }

  onCarouselTouchStart(event: TouchEvent): void {
    const touch = event.touches[0];
    if (!touch || !this.product) {
      return;
    }
    this.touchStartX.set(this.product.id, touch.clientX);
  }

  onCarouselTouchEnd(event: TouchEvent): void {
    const product = this.product;
    if (!product) {
      return;
    }

    const startX = this.touchStartX.get(product.id);
    this.touchStartX.delete(product.id);
    if (startX === undefined) {
      return;
    }

    const touch = event.changedTouches[0];
    if (!touch) {
      return;
    }

    const deltaX = touch.clientX - startX;
    if (Math.abs(deltaX) < 35) {
      return;
    }

    if (deltaX < 0) {
      this.nextCarouselImage();
    } else {
      this.previousCarouselImage();
    }
  }

  openImage(url: string): void {
    this.selectedImageUrl = url;
    this.zoomedImageUrl = url;
  }

  closeZoom(): void {
    this.zoomedImageUrl = '';
  }

  resolveMediaUrl(url?: string | null): string {
    return url ?? '';
  }

  formatDateTime(value: string | null | undefined): string {
    if (!value) {
      return '-';
    }
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
      return value;
    }
    return new Intl.DateTimeFormat('it-IT', {
      dateStyle: 'medium',
      timeStyle: 'short',
    }).format(date);
  }

  addToCart(): void {
    if (!this.product) {
      return;
    }
    this.cart.addProduct({
      id: this.product.id,
      productCode: this.product.productCode,
      slug: this.product.slug,
      name: this.product.name,
      shortDescription: this.product.shortDescription,
      listPrice: this.product.listPrice,
      salePrice: this.product.salePrice,
      effectivePrice: this.product.effectivePrice,
      currencyCode: this.product.currencyCode,
      thumbnailUrl: this.product.images[0]?.imageUrl ?? null,
      brandName: this.product.brandName,
      categoryName: this.product.categoryName,
      categorySlug: this.product.categorySlug,
      images: this.product.images,
    });
  }

  buyNow(): void {
    if (!this.product) {
      return;
    }

    const email = this.session.user()?.email ?? '';
    if (!email) {
      this.quickBuyError = 'Devi accedere con un account prima di acquistare.';
      this.cdr.markForCheck();
      return;
    }

    this.quickBuyLoading = true;
    this.quickBuyError = '';
    this.cdr.markForCheck();

    this.paymentApi.createQuickBuy(email, {
      productId: this.product.id,
      productVariantId: null,
      quantity: 1,
      variantLabel: null,
    }).subscribe({
      next: (response) => {
        window.location.assign(response.url);
      },
      error: (error) => {
        this.quickBuyLoading = false;
        const summary = summarizeHttpError(error, 'Creazione acquisto rapido fallita.');
        this.quickBuyError = summary.message;
        this.cdr.markForCheck();
      },
    });
  }

  formatMoney(value: number | null | undefined, currencyCode: string): string {
    if (value === null || value === undefined) {
      return '-';
    }
    return new Intl.NumberFormat('it-IT', {
      style: 'currency',
      currency: currencyCode || 'EUR',
    }).format(value);
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    this.closeZoom();
  }
}
