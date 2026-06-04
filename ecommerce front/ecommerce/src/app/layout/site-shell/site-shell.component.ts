import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject } from '@angular/core';
import { Router, RouterLink, RouterOutlet } from '@angular/router';
import { SessionService } from '../../core/services/session.service';
import { CategoryApiService } from '../../core/services/category-api.service';
import { CategoryNavigationResponse } from '../../core/models/category.models';
import { ShopCatalogService } from '../../core/services/shop-catalog.service';
import { CartService } from '../../core/services/cart.service';

@Component({
  selector: 'app-site-shell',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterOutlet],
  templateUrl: './site-shell.component.html',
  styleUrl: './site-shell.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SiteShellComponent implements OnInit {
  protected readonly session = inject(SessionService);
  protected readonly cart = inject(CartService);
  private readonly router = inject(Router);
  private readonly categoryApi = inject(CategoryApiService);
  private readonly shopCatalog = inject(ShopCatalogService);

  protected categories: CategoryNavigationResponse[] = [];
  protected categoriesLoading = true;
  protected categoriesError = '';
  protected shopMenuOpen = false;

  readonly primaryNav = [
    { label: 'Home', route: '/' },
    { label: 'Shop', route: '/shop' },
  ];

  ngOnInit(): void {
    this.loadCategories();
  }

  refreshCategories(): void {
    this.loadCategories(true);
  }

  openShopMenu(): void {
    this.shopMenuOpen = true;
  }

  closeShopMenu(): void {
    this.shopMenuOpen = false;
  }

  openShop(event: MouseEvent): void {
    event.preventDefault();
    this.shopCatalog.refresh(null);
    void this.router.navigateByUrl(`/shop?refresh=${Date.now()}`);
  }

  trackByCategoryId(_: number, category: CategoryNavigationResponse): number {
    return category.id;
  }

  logout(): void {
    this.session.logout();
  }

  private loadCategories(forceRefresh = false): void {
    this.categoriesLoading = true;
    this.categoriesError = '';
    const request$ = forceRefresh ? this.categoryApi.refreshNavigationTree() : this.categoryApi.listNavigationTree();

    request$.subscribe({
      next: (categories) => {
        this.categories = categories;
        this.categoriesLoading = false;
        if (!categories.length) {
          this.categoriesError = 'No categories available in the menu.';
        }
      },
      error: () => {
        this.categoriesLoading = false;
        this.categories = [];
        this.categoriesError = 'Category menu is unavailable. Try again or refresh the page.';
      },
    });
  }
}