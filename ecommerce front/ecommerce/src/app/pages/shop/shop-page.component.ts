import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, DestroyRef, OnInit, inject } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { catchError, finalize, of, timeout } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CategoryNavigationResponse } from '../../core/models/category.models';
import { ProductImageResponse, ProductSummaryResponse } from '../../core/models/product.models';
import { StripeCheckoutItemRequest } from '../../core/models/payment.models';
import { CategoryApiService } from '../../core/services/category-api.service';
import { CartService } from '../../core/services/cart.service';
import { PaymentApiService } from '../../core/services/payment-api.service';
import { ShopCatalogService, ShopCatalogState } from '../../core/services/shop-catalog.service';
import { SessionService } from '../../core/services/session.service';
import { summarizeHttpError } from '../../core/utils/http-errors';

interface CategorySection {
  category: CategoryNavigationResponse;
  products: ProductSummaryResponse[];
}

interface ShopSection {
  root: CategoryNavigationResponse;
  categoriess: CategorySection[];
}

interface CategoryFilterItem {
  label: string;
  slug: string;
}

interface CategoryFilterGroup {
  rootLabel: string;
  children: CategoryFilterItem[];
}

interface CardImage {
  imageUrl: string;
  altText: string;
  primary: boolean;
}

@Component({
  selector: 'app-shop-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './shop-page.component.html',
  styleUrl: './shop-page.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ShopPageComponent implements OnInit {
  private static readonly requestTimeoutMs = 6000;
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly categoryApi = inject(CategoryApiService);
  private readonly cart = inject(CartService);
  private readonly paymentApi = inject(PaymentApiService);
  private readonly shopCatalog = inject(ShopCatalogService);
  private readonly session = inject(SessionService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly cdr = inject(ChangeDetectorRef);

  protected errorMessage = '';
  protected allProducts: ProductSummaryResponse[] = [];
  protected products: ProductSummaryResponse[] = [];
  protected categoriess: CategoryNavigationResponse[] = [];
  protected sections: ShopSection[] = [];
  protected fallbackProducts: ProductSummaryResponse[] = [];
  protected categoryGroups: CategoryFilterGroup[] = [];
  protected selectedCategorySlug = '';
  protected loadingCategories = true;
  protected loadingProducts = true;
  protected readonly carouselIndex = new Map<number, number>();
  protected quickBuyError = '';
  protected quickBuyLoadingId: number | null = null;
  private readonly selectableCategorySlugs = new Set<string>();
  private categoryWatchdogId: number | null = null;
  private productWatchdogId: number | null = null;
  private touchStartX = new Map<number, number>();

  protected get loading(): boolean {
    return this.loadingProducts;
  }

  protected get isStaff(): boolean {
    return this.session.isStaff();
  }

  ngOnInit(): void {
    this.selectedCategorySlug = this.route.snapshot.queryParamMap.get('category') ?? '';
    this.loadCategories();
    this.shopCatalog.state$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((state) => {
      this.syncCatalogState(state);
    });
    this.shopCatalog.refresh();
    this.route.queryParamMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((params) => {
      const nextCategory = this.normalizeSelectableCategory(params.get('category'));
      if (nextCategory === this.selectedCategorySlug) {
        return;
      }
      this.selectedCategorySlug = nextCategory;
      this.applyCategoryFilter();
    });
  }

  clearCategory(): void {
    this.selectCategory(null);
  }

  selectCategory(categorySlug: string | null): void {
    this.selectedCategorySlug = this.normalizeSelectableCategory(categorySlug);
    this.applyCategoryFilter();
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {
        category: this.selectedCategorySlug || null,
      },
      queryParamsHandling: 'merge',
    });
  }

  resolveMediaUrl(url?: string | null): string {
    return url ?? '';
  }

  formatMoney(value: number | null | undefined, currencyCode: string): string {
    if (value === null || value === undefined) {
      return '-';
    }
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: currencyCode || 'EUR',
    }).format(value);
  }

  addToCart(product: ProductSummaryResponse): void {
    this.cart.addProduct(product);
  }

  buyNow(product: ProductSummaryResponse): void {
    const email = this.session.user()?.email ?? '';
    if (!email) {
      this.quickBuyError = 'You must sign in with an account before purchasing.';
      this.cdr.markForCheck();
      return;
    }

    const payload: StripeCheckoutItemRequest = {
      productId: product.id,
      productVariantId: null,
      quantity: 1,
      variantLabel: null,
    };

    this.quickBuyLoadingId = product.id;
    this.quickBuyError = '';
    this.cdr.markForCheck();

    this.paymentApi.createQuickBuy(email, payload).subscribe({
      next: (response) => {
        window.location.assign(response.url);
      },
      error: (error) => {
        this.quickBuyLoadingId = null;
        this.quickBuyError = summarizeHttpError(error, 'Quick purchase creation failed.').message;
        this.cdr.markForCheck();
      },
    });
  }

  trackByRootId(_: number, section: ShopSection): number {
    return section.root.id;
  }

  trackByCategoryId(_: number, section: CategorySection): number {
    return section.category.id;
  }

  trackByProductId(_: number, product: ProductSummaryResponse): number {
    return product.id;
  }

  cardImages(product: ProductSummaryResponse): CardImage[] {
    if (product.images?.length) {
      return product.images.map((image: ProductImageResponse) => ({
        imageUrl: image.imageUrl,
        altText: image.altText || product.name,
        primary: Boolean(image.primary),
      }));
    }

    if (product.thumbnailUrl) {
      return [
        {
          imageUrl: product.thumbnailUrl,
          altText: product.name,
          primary: true,
        },
      ];
    }

    return [];
  }

  cardImage(product: ProductSummaryResponse): CardImage | null {
    const images = this.cardImages(product);
    if (!images.length) {
      return null;
    }
    return images[0] ?? null;
  }

  isFilterActive(slug: string): boolean {
    return this.selectedCategorySlug === slug;
  }

  selectedCategoryLabel(): string {
    if (!this.selectedCategorySlug) {
      return 'All products';
    }
    for (const root of this.categoriess) {
      const child = root.children.find((item) => item.slug === this.selectedCategorySlug);
      if (child) {
        return `${root.name} / ${child.name}`;
      }
    }
    return this.selectedCategorySlug;
  }

  private loadCategories(): void {
    this.loadingCategories = true;
    this.armWatchdog('categoriess');
    this.categoryApi
      .listNavigationTree()
      .pipe(
        timeout({ first: ShopPageComponent.requestTimeoutMs }),
        catchError((error) => {
          this.errorMessage = summarizeHttpError(error, 'Category loading failed.').message;
          this.categoryGroups = [];
          this.selectableCategorySlugs.clear();
          return of([] as CategoryNavigationResponse[]);
        }),
        finalize(() => this.disarmWatchdog('categoriess')),
      )
      .subscribe({
        next: (categoriess) => {
          this.categoriess = categoriess;
          this.categoryGroups = this.buildCategoryGroups(categoriess);
          this.selectableCategorySlugs.clear();
          for (const group of this.categoryGroups) {
            for (const child of group.children) {
              this.selectableCategorySlugs.add(child.slug);
            }
          }
          const normalizedSelectedCategory = this.normalizeSelectableCategory(this.selectedCategorySlug);
          if (normalizedSelectedCategory !== this.selectedCategorySlug) {
            this.selectedCategorySlug = normalizedSelectedCategory;
            this.shopCatalog.refresh(this.selectedCategorySlug || null);
          }
          this.loadingCategories = false;
          this.rebuildSections();
        },
      });
  }

  private loadProducts(): void {
    this.shopCatalog.refresh();
  }

  private syncCatalogState(state: ShopCatalogState): void {
    this.allProducts = state.products;
    this.loadingProducts = state.loading;
    this.errorMessage = state.error;
    this.applyCategoryFilter();
    this.rebuildSections();
  }

  private applyCategoryFilter(): void {
    if (!this.selectedCategorySlug) {
      this.products = this.allProducts;
      return;
    }

    this.products = this.allProducts.filter((product) => product.categorySlug === this.selectedCategorySlug);
  }

  private rebuildSections(): void {
    const productMap = new Map<string, ProductSummaryResponse[]>();
    const categorySlugs = new Set<string>();
    this.collectCategorySlugs(this.categoriess, categorySlugs);
    const fallbackProducts: ProductSummaryResponse[] = [];

    for (const product of this.products) {
      if (!product.categorySlug || !categorySlugs.has(product.categorySlug)) {
        fallbackProducts.push(product);
        continue;
      }
      const bucket = productMap.get(product.categorySlug) ?? [];
      bucket.push(product);
      productMap.set(product.categorySlug, bucket);
    }

    this.sections = this.categoriess
      .filter((root) => this.shouldShowRoot(root))
      .map((root) => ({
        root,
        categoriess: this.visibleChildren(root).map((child) => ({
          category: child,
          products: productMap.get(child.slug) ?? [],
        })),
      }));
    this.fallbackProducts = this.selectedCategorySlug ? [] : fallbackProducts;
  }

  private shouldShowRoot(root: CategoryNavigationResponse): boolean {
    if (!this.selectedCategorySlug) {
      return true;
    }
    if (root.slug === this.selectedCategorySlug) {
      return true;
    }
    return root.children.some((child) => child.slug === this.selectedCategorySlug);
  }

  private visibleChildren(root: CategoryNavigationResponse): CategoryNavigationResponse[] {
    if (!this.selectedCategorySlug) {
      return root.children;
    }
    if (root.slug === this.selectedCategorySlug) {
      return root.children;
    }
    const child = root.children.find((item) => item.slug === this.selectedCategorySlug);
    return child ? [child] : [];
  }

  private collectCategorySlugs(categoriess: CategoryNavigationResponse[], target: Set<string>): void {
    for (const category of categoriess) {
      target.add(category.slug);
      if (category.children.length) {
        this.collectCategorySlugs(category.children, target);
      }
    }
  }

  private buildCategoryGroups(categoriess: CategoryNavigationResponse[]): CategoryFilterGroup[] {
    return categoriess.map((root) => ({
      rootLabel: root.name,
      children: root.children.map((child) => ({
        label: child.name,
        slug: child.slug,
      })),
    }));
  }

  private normalizeSelectableCategory(categorySlug: string | null): string {
    if (!categorySlug) {
      return '';
    }
    if (!this.selectableCategorySlugs.size) {
      return categorySlug;
    }
    return this.selectableCategorySlugs.has(categorySlug) ? categorySlug : '';
  }

  private armWatchdog(kind: 'categoriess' | 'products'): void {
    this.disarmWatchdog(kind);
    const label = kind === 'categoriess' ? 'categories' : 'products';
    const id = window.setTimeout(() => {
      if (kind === 'categoriess' && this.loadingCategories) {
        this.categoriess = [];
        this.categoryGroups = [];
        this.selectableCategorySlugs.clear();
        this.loadingCategories = false;
        this.errorMessage = `Loading ${label} did not complete. Verify that the backend is responding.`;
        this.rebuildSections();
      }
      if (kind === 'products' && this.loadingProducts) {
        this.products = [];
        this.loadingProducts = false;
        this.errorMessage = `Loading ${label} did not complete. Verify that the backend is responding.`;
        this.rebuildSections();
      }
    }, ShopPageComponent.requestTimeoutMs + 1000);

    if (kind === 'categoriess') {
      this.categoryWatchdogId = id;
    } else {
      this.productWatchdogId = id;
    }
  }

  private disarmWatchdog(kind: 'categoriess' | 'products'): void {
    const id = kind === 'categoriess' ? this.categoryWatchdogId : this.productWatchdogId;
    if (id !== null) {
      window.clearTimeout(id);
    }
    if (kind === 'categoriess') {
      this.categoryWatchdogId = null;
    } else {
      this.productWatchdogId = null;
    }
  }
}
