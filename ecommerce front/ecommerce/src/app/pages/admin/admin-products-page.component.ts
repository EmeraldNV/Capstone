import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { CategoryNavigationResponse } from '../../core/models/category.models';
import { TagliaResponse } from '../../core/models/taglia.models';
import { CategoryApiService } from '../../core/services/category-api.service';
import { ProductApiService } from '../../core/services/product-api.service';
import { TagliaService } from '../../core/services/taglia.service';
import { summarizeHttpError } from '../../core/utils/http-errors';
import { ProductDetailResponse, ProductSummaryResponse, ProductUpsertRequest } from '../../core/models/product.models';
import { ProductImageManagerComponent } from './product-image-manager.component';

@Component({
  selector: 'app-admin-products-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, ProductImageManagerComponent],
  templateUrl: './admin-products-page.component.html',
  styleUrl: './admin-products-page.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdminProductsPageComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(ProductApiService);
  private readonly tagliaApi = inject(TagliaService);
  private readonly categoryApi = inject(CategoryApiService);

  protected loading = false;
  protected saving = false;
  protected uploading = false;
  protected message = '';
  protected errorMessage = '';
  protected errorDetails: string[] = [];
  protected products: ProductSummaryResponse[] = [];
  protected editingProduct: ProductDetailResponse | null = null;
  protected selectedFiles: Array<Blob & { name?: string }> = [];
  protected stagedResetToken = 0;
  protected availableTaglie: TagliaResponse[] = [];
  protected taglieLoading = true;
  protected taglieError = '';
  protected selectedCatalogTagliaId: number | null = null;
  protected categoryTree: CategoryNavigationResponse[] = [];
  protected categoriesLoading = true;
  protected categoriesError = '';
  private readonly allowedCategoryIds = new Set<number>();
  private readonly allowedTagliaIds = new Set<number>();
  readonly productClassificationOptions = [
    { value: 'FOOTWEAR_SHOES', label: 'Footwear / Shoes' },
    { value: 'FOOTWEAR_SOCKS', label: 'Footwear / Socks' },
    { value: 'APPAREL_TSHIRT', label: 'Apparel / T-Shirt' },
    { value: 'APPAREL_CROP_TOP', label: 'Apparel / Crop Top' },
  ];

  readonly genders = ['MEN', 'WOMEN', 'UNISEX', 'KIDS'];
  readonly ageGroups = ['ADULT', 'JUNIOR', 'KIDS', 'BABY'];
  readonly seasons = ['ALL_SEASON', 'WINTER', 'SPRING', 'SUMMER', 'AUTUMN'];

  readonly form = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(200)]],
    shortDescription: ['', [Validators.maxLength(500)]],
    description: [''],
    sportType: ['FOOTWEAR_SHOES', [Validators.required]],
    gender: ['UNISEX', [Validators.required]],
    ageGroup: ['ADULT', [Validators.required]],
    season: [''],
    material: [''],
    careInstructions: [''],
    taxable: [true, [Validators.required]],
    ipfApproved: [false, [Validators.required]],
    active: [true, [Validators.required]],
    listPrice: [0, [Validators.required, Validators.min(0)]],
    salePrice: [null as number | null],
    currencyCode: ['EUR', [Validators.required, Validators.minLength(3), Validators.maxLength(3)]],
    stockQuantity: [0, [Validators.required, Validators.min(0)]],
    brandId: [null as number | null],
    categoryId: [null as number | null, [Validators.required, this.categorySelectionValidator()]],
    tagliaPicker: [null as number | null],
    tagliaIds: [[] as number[], [Validators.required, this.tagliaSelectionValidator()]],
  });

  ngOnInit(): void {
    this.loadTaglie();
    this.loadCategories();
    this.loadProducts();
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const request = this.buildRequest();
    const wasEditing = Boolean(this.editingProduct);
    this.saving = true;
    this.errorMessage = '';
    this.errorDetails = [];
    this.message = '';

    const action$ = this.editingProduct
      ? this.api.updateProduct(this.editingProduct.id, request)
      : this.api.createProduct(request);

    action$.subscribe({
      next: (product) => {
        this.editingProduct = product;
        if (this.selectedFiles.length) {
          this.uploadPendingImages(product.id, wasEditing);
          return;
        }

        this.saving = false;
        this.message = wasEditing ? 'Prodotto aggiornato.' : 'Prodotto creato.';
        this.selectedFiles = [];
        this.loadProducts();
        if (wasEditing) {
          this.patchFormFromProduct(product);
        } else {
          this.clearEditor();
        }
      },
      error: (error) => {
        this.saving = false;
        const summary = summarizeHttpError(error, 'Salvataggio prodotto fallito.');
        this.errorMessage = summary.message;
        this.errorDetails = summary.details;
      },
    });
  }

  editProduct(productId: number): void {
    this.loading = true;
    this.errorMessage = '';
    this.errorDetails = [];
    this.api.getAdminProduct(productId).subscribe({
      next: (product) => {
        this.loading = false;
        this.editingProduct = product;
        this.resetStagedFiles();
        this.form.patchValue({
          name: product.name,
          shortDescription: product.shortDescription ?? '',
          description: product.description ?? '',
          sportType: product.sportType,
          gender: product.gender,
          ageGroup: product.ageGroup,
          season: product.season ?? '',
          material: product.material ?? '',
          careInstructions: product.careInstructions ?? '',
          taxable: product.taxable,
          ipfApproved: product.ipfApproved,
          active: product.active,
          listPrice: product.listPrice,
          salePrice: product.salePrice ?? null,
          currencyCode: product.currencyCode,
          stockQuantity: product.stockQuantity,
          brandId: product.brandId ?? null,
          categoryId: product.categoryId ?? null,
          tagliaIds: (product.taglie ?? []).map((taglia) => taglia.id),
        });
        this.form.markAsPristine();
      },
      error: (error) => {
        this.loading = false;
        const summary = summarizeHttpError(error, 'Caricamento prodotto fallito.');
        this.errorMessage = summary.message;
        this.errorDetails = summary.details;
      },
    });
  }

  deleteProduct(productId: number): void {
    if (!window.confirm('Eliminare questo prodotto?')) {
      return;
    }
    this.saving = true;
    this.api.deleteProduct(productId).subscribe({
      next: () => {
        this.saving = false;
        this.message = 'Prodotto eliminato.';
        if (this.editingProduct?.id === productId) {
          this.clearEditor();
        }
        this.loadProducts();
      },
      error: (error) => {
        this.saving = false;
        const summary = summarizeHttpError(error, 'Eliminazione prodotto fallita.');
        this.errorMessage = summary.message;
        this.errorDetails = summary.details;
      },
    });
  }

  onFilesSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.selectedFiles = Array.from(input.files ?? []);
  }

  onStagedFilesChanged(files: Array<Blob & { name?: string }>): void {
    this.selectedFiles = files;
  }

  removeImage(imageId: number): void {
    if (!this.editingProduct) {
      return;
    }
    this.uploading = true;
    this.api.deleteImage(this.editingProduct.id, imageId).subscribe({
      next: (product) => {
        this.uploading = false;
        this.editingProduct = product;
        this.loadProducts();
      },
      error: (error) => {
        this.uploading = false;
        const summary = summarizeHttpError(error, 'Rimozione immagine fallita.');
        this.errorMessage = summary.message;
        this.errorDetails = summary.details;
      },
    });
  }

  setPrimaryImage(imageId: number): void {
    if (!this.editingProduct) {
      return;
    }
    this.uploading = true;
    this.api.setPrimaryImage(this.editingProduct.id, imageId).subscribe({
      next: (product) => {
        this.uploading = false;
        this.editingProduct = product;
        this.loadProducts();
      },
      error: (error) => {
        this.uploading = false;
        const summary = summarizeHttpError(error, 'Impostazione immagine principale fallita.');
        this.errorMessage = summary.message;
        this.errorDetails = summary.details;
      },
    });
  }

  saveImageOrder(imageIds: number[]): void {
    if (!this.editingProduct) {
      return;
    }
    this.uploading = true;
    this.api.reorderImages(this.editingProduct.id, imageIds).subscribe({
      next: (product) => {
        this.uploading = false;
        this.editingProduct = product;
        this.message = 'Ordine immagini aggiornato.';
        this.loadProducts();
      },
      error: (error) => {
        this.uploading = false;
        const summary = summarizeHttpError(error, 'Riordino immagini fallito.');
        this.errorMessage = summary.message;
        this.errorDetails = summary.details;
      },
    });
  }

  cancelEditing(): void {
    this.clearEditor();
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

  resolveMediaUrl(url?: string | null): string {
    return url ?? '';
  }

  trackByProductId(_: number, product: ProductSummaryResponse): number {
    return product.id;
  }

  trackByImageId(_: number, image: { id: number }): number {
    return image.id;
  }

  trackByTagliaId(_: number, taglia: TagliaResponse): number {
    return taglia.id;
  }

  onTagliaPickerChange(): void {
    const picker = this.form.get('tagliaPicker');
    const rawValue = picker?.value;
    const tagliaId = rawValue == null ? null : Number(rawValue);
    if (tagliaId == null || Number.isNaN(tagliaId) || this.isTagliaSelected(tagliaId)) {
      return;
    }

    const tagliaIds = [...((this.form.get('tagliaIds')?.value ?? []) as number[])];
    tagliaIds.push(tagliaId);
    this.form.get('tagliaIds')?.setValue(tagliaIds);
    picker?.setValue(null);
    picker?.markAsPristine();
  }

  isTagliaSelected(tagliaId: number): boolean {
    const tagliaIds = (this.form.get('tagliaIds')?.value ?? []) as number[];
    return tagliaIds.map((id) => Number(id)).includes(tagliaId);
  }

  tagliaById(tagliaId: number): TagliaResponse | undefined {
    return this.availableTaglie.find((taglia) => taglia.id === tagliaId);
  }

  removeTaglia(tagliaId: number): void {
    const tagliaIds = ((this.form.get('tagliaIds')?.value ?? []) as number[]).filter((id) => Number(id) !== tagliaId);
    this.form.get('tagliaIds')?.setValue(tagliaIds);
    this.form.get('tagliaIds')?.markAsDirty();
  }

  private loadProducts(): void {
    this.loading = true;
    if (this.selectedCatalogTagliaId == null) {
      this.api.listAdminProducts().subscribe({
        next: (products) => {
          this.loading = false;
          this.products = products;
        },
        error: (error) => {
          this.loading = false;
          const summary = summarizeHttpError(error, 'Caricamento lista prodotti fallito.');
          this.errorMessage = summary.message;
          this.errorDetails = summary.details;
        },
      });
      return;
    }

    this.tagliaApi.getProductsByTaglia(this.selectedCatalogTagliaId, { page: 0, size: 50 }).subscribe({
      next: (page) => {
        this.loading = false;
        this.products = page.content;
      },
      error: (error) => {
        this.loading = false;
        const summary = summarizeHttpError(error, 'Caricamento prodotti filtrati fallito.');
        this.errorMessage = summary.message;
        this.errorDetails = summary.details;
      },
    });
  }

  protected onCatalogTagliaChange(event: Event): void {
    const select = event.target as HTMLSelectElement;
    const rawValue = select.value;
    this.selectedCatalogTagliaId = rawValue ? Number(rawValue) : null;
    this.loading = true;
    this.loadProducts();
  }

  protected leafCategories(): CategoryNavigationResponse[] {
    return this.categoryTree.flatMap((root) => root.children);
  }

  protected trackByCategoryId(_: number, category: CategoryNavigationResponse): number {
    return category.id;
  }

  private loadCategories(): void {
    this.categoriesLoading = true;
    this.categoriesError = '';
    this.categoryApi.listNavigationTree().subscribe({
      next: (categories) => {
        this.categoryTree = categories;
        this.allowedCategoryIds.clear();
        for (const category of this.leafCategories()) {
          this.allowedCategoryIds.add(category.id);
        }
        this.ensureDefaultCategorySelection();
        this.categoriesLoading = false;
        this.form.get('categoryId')?.updateValueAndValidity({ onlySelf: true, emitEvent: false });
        if (!categories.length) {
          this.categoriesError = 'Nessuna categoria disponibile nel menu.';
        }
      },
      error: () => {
        this.categoriesLoading = false;
        this.categoryTree = [];
        this.allowedCategoryIds.clear();
        this.categoriesError = 'Caricamento categorie fallito. Verifica il backend o ricarica la pagina.';
        this.form.get('categoryId')?.updateValueAndValidity({ onlySelf: true, emitEvent: false });
      },
    });
  }

  private loadTaglie(): void {
    this.taglieLoading = true;
    this.taglieError = '';
    this.tagliaApi.getTaglie().subscribe({
      next: (taglie) => {
        this.availableTaglie = taglie;
        this.allowedTagliaIds.clear();
        for (const taglia of taglie) {
          this.allowedTagliaIds.add(taglia.id);
        }
        this.taglieLoading = false;
        this.form.get('tagliaIds')?.updateValueAndValidity({ onlySelf: true, emitEvent: false });
      },
      error: () => {
        this.taglieLoading = false;
        this.availableTaglie = [];
        this.allowedTagliaIds.clear();
        this.taglieError = 'Caricamento taglie fallito. Verifica il backend o ricarica la pagina.';
        this.form.get('tagliaIds')?.updateValueAndValidity({ onlySelf: true, emitEvent: false });
      },
    });
  }

  private uploadPendingImages(productId: number, preserveEditing: boolean): void {
    if (!this.selectedFiles.length) {
      this.saving = false;
      return;
    }

    this.uploading = true;
    this.api.uploadImages(productId, this.selectedFiles).subscribe({
      next: (product) => {
        this.uploading = false;
        this.saving = false;
        this.editingProduct = product;
        this.message = 'Prodotto salvato e immagini caricate.';
        this.resetStagedFiles();
        this.loadProducts();
        if (preserveEditing) {
          this.patchFormFromProduct(product);
        } else {
          this.clearEditor();
        }
      },
      error: (error) => {
        this.uploading = false;
        this.saving = false;
        const summary = summarizeHttpError(error, 'Upload immagini fallito.');
        this.errorMessage = summary.message;
        this.errorDetails = summary.details;
      },
    });
  }

  private clearEditor(): void {
    this.editingProduct = null;
    this.resetStagedFiles();
    this.form.reset({
      name: '',
      shortDescription: '',
      description: '',
      sportType: 'FOOTWEAR_SHOES',
      gender: 'UNISEX',
      ageGroup: 'ADULT',
      season: '',
      material: '',
      careInstructions: '',
      taxable: true,
      ipfApproved: false,
      active: true,
      listPrice: 0,
      salePrice: null,
      currencyCode: 'EUR',
      stockQuantity: 0,
      brandId: null,
      categoryId: null,
      tagliaPicker: null,
      tagliaIds: [] as number[],
    });
    this.ensureDefaultCategorySelection();
    this.form.markAsPristine();
  }

  private patchFormFromProduct(product: ProductDetailResponse): void {
    this.form.patchValue({
      name: product.name,
      shortDescription: product.shortDescription ?? '',
      description: product.description ?? '',
      sportType: this.normalizeClassificationValue(product.sportType, product.categorySlug),
      gender: product.gender,
      ageGroup: product.ageGroup,
      season: product.season ?? '',
      material: product.material ?? '',
      careInstructions: product.careInstructions ?? '',
      taxable: product.taxable,
      ipfApproved: product.ipfApproved,
      active: product.active,
      listPrice: product.listPrice,
      salePrice: product.salePrice ?? null,
      currencyCode: product.currencyCode,
      stockQuantity: product.stockQuantity,
      brandId: product.brandId ?? null,
      categoryId: product.categoryId ?? null,
      tagliaIds: (product.taglie ?? []).map((taglia) => taglia.id),
    });
    this.form.markAsPristine();
  }

  private buildRequest(): ProductUpsertRequest {
    const value = this.form.getRawValue();
    return {
      name: value.name ?? '',
      shortDescription: value.shortDescription || null,
      description: value.description || null,
      sportType: this.normalizeClassificationValue(value.sportType, null),
      gender: value.gender ?? 'UNISEX',
      ageGroup: value.ageGroup ?? 'ADULT',
      season: value.season || null,
      material: value.material || null,
      careInstructions: value.careInstructions || null,
      taxable: Boolean(value.taxable),
      ipfApproved: Boolean(value.ipfApproved),
      active: Boolean(value.active),
      listPrice: Number(value.listPrice ?? 0),
      salePrice: value.salePrice == null ? null : Number(value.salePrice),
      currencyCode: (value.currencyCode ?? 'EUR').toUpperCase(),
      stockQuantity: Number(value.stockQuantity ?? 0),
      brandId: value.brandId == null ? null : Number(value.brandId),
      categoryId: value.categoryId == null ? null : Number(value.categoryId),
      tagliaIds: (value.tagliaIds ?? []).map((tagliaId) => Number(tagliaId)),
    };
  }

  private ensureDefaultCategorySelection(): void {
    const categoryControl = this.form.get('categoryId');
    if (!categoryControl) {
      return;
    }

    const currentValue = categoryControl.value;
    if (currentValue !== null && currentValue !== undefined) {
      return;
    }

    const firstCategory = this.leafCategories()[0];
    if (firstCategory) {
      categoryControl.setValue(firstCategory.id, { emitEvent: false });
      categoryControl.markAsPristine();
    }
  }

  private resetStagedFiles(): void {
    this.selectedFiles = [];
    this.stagedResetToken++;
  }

  private categorySelectionValidator(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      const rawValue = control.value;
      if (rawValue === null || rawValue === undefined || rawValue === '') {
        return { required: true };
      }

      const id = Number(rawValue);
      if (Number.isNaN(id)) {
        return { categoryInvalid: true };
      }

      return this.allowedCategoryIds.has(id) ? null : { categoryInvalid: true };
    };
  }

  private tagliaSelectionValidator(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      const rawValue = control.value;
      if (!Array.isArray(rawValue) || rawValue.length === 0) {
        return { required: true };
      }

      const ids = rawValue.map((value) => Number(value));
      if (ids.some((id) => Number.isNaN(id))) {
        return { tagliaInvalid: true };
      }

      return ids.every((id) => this.allowedTagliaIds.has(id)) ? null : { tagliaInvalid: true };
    };
  }

  protected productClassificationLabel(value: string | null | undefined): string {
    const match = this.productClassificationOptions.find((item) => item.value === value);
    return match?.label ?? value ?? '-';
  }

  private normalizeClassificationValue(value: string | null | undefined, categorySlug: string | null | undefined): string {
    const normalized = (value ?? '').trim().toUpperCase();
    if (this.productClassificationOptions.some((item) => item.value === normalized)) {
      return normalized;
    }

    const slug = (categorySlug ?? '').trim().toLowerCase();
    if (slug.includes('socks')) {
      return 'FOOTWEAR_SOCKS';
    }
    if (slug.includes('shoe')) {
      return 'FOOTWEAR_SHOES';
    }
    if (slug.includes('crop')) {
      return 'APPAREL_CROP_TOP';
    }
    if (slug.includes('shirt') || slug.includes('tshirt')) {
      return 'APPAREL_TSHIRT';
    }

    return 'FOOTWEAR_SHOES';
  }
}
