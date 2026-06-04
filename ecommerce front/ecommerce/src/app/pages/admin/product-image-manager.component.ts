import { CommonModule } from '@angular/common';
import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  EventEmitter,
  Input,
  OnChanges,
  OnDestroy,
  Output,
  SimpleChanges,
  ViewChild,
} from '@angular/core';
import type { FilePondFile } from 'filepond';
import { ProductImageResponse } from '../../core/models/product.models';

@Component({
  selector: 'app-product-image-manager',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './product-image-manager.component.html',
  styleUrl: './product-image-manager.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProductImageManagerComponent implements AfterViewInit, OnChanges, OnDestroy {
  @Input() images: ProductImageResponse[] = [];
  @Input() productName = '';
  @Input() disabled = false;
  @Input() stagedResetToken = 0;

  @Output() stagedFilesChange = new EventEmitter<Array<Blob & { name?: string }>>();
  @Output() reorderRequested = new EventEmitter<number[]>();
  @Output() primaryRequested = new EventEmitter<number>();
  @Output() removeRequested = new EventEmitter<number>();

  @ViewChild('pondHost', { static: true }) private pondHost?: ElementRef<HTMLElement>;

  protected orderedImages: ProductImageResponse[] = [];
  protected stagedFileNames: string[] = [];
  protected orderDirty = false;
  protected dragIndex: number | null = null;
  protected selectedPrimaryImageId: number | null = null;

  private pond: { removeFiles?: () => void; setOptions?: (options: { disabled: boolean }) => void } | null = null;

  ngAfterViewInit(): void {
    this.syncImages();
    void this.initPond();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['images']) {
      this.syncImages();
    }

    if (changes['stagedResetToken'] && !changes['stagedResetToken'].firstChange) {
      this.clearStagedFiles();
    }

    if (changes['disabled'] && this.pond?.setOptions) {
      this.pond.setOptions({ disabled: this.disabled });
    }
  }

  onDragStart(index: number, event: DragEvent): void {
    if (this.disabled) {
      return;
    }
    this.dragIndex = index;
    event.dataTransfer?.setData('text/plain', String(index));
    event.dataTransfer?.setDragImage(event.currentTarget as HTMLElement, 16, 16);
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
  }

  onDrop(index: number, event: DragEvent): void {
    event.preventDefault();
    if (this.disabled || this.dragIndex === null || this.dragIndex === index) {
      this.dragIndex = null;
      return;
    }

    const next = [...this.orderedImages];
    const [moved] = next.splice(this.dragIndex, 1);
    next.splice(index, 0, moved);
    this.orderedImages = next;
    this.dragIndex = null;
    this.orderDirty = true;
  }

  onDragEnd(): void {
    this.dragIndex = null;
  }

  saveOrder(): void {
    if (!this.orderDirty || this.disabled || !this.orderedImages.length) {
      return;
    }
    this.reorderRequested.emit(this.orderedImages.map((image) => image.id));
  }

  onPrimarySelectionChange(value: string): void {
    if (this.disabled) {
      return;
    }

    const imageId = Number(value);
    if (Number.isNaN(imageId)) {
      return;
    }

    this.selectedPrimaryImageId = imageId;
    this.primaryRequested.emit(imageId);
  }

  handlePrimary(imageId: number): void {
    if (!this.disabled) {
      this.primaryRequested.emit(imageId);
    }
  }

  handleRemove(imageId: number): void {
    if (!this.disabled) {
      this.removeRequested.emit(imageId);
    }
  }

  private clearStagedFiles(): void {
    this.stagedFileNames = [];
    this.stagedFilesChange.emit([]);
    this.pond?.removeFiles?.();
  }

  private syncImages(): void {
    this.orderedImages = [...(this.images ?? [])];
    this.orderDirty = false;
    this.dragIndex = null;
    this.selectedPrimaryImageId =
      this.orderedImages.find((image) => image.primary)?.id ?? this.orderedImages[0]?.id ?? null;
  }

  private async initPond(): Promise<void> {
    if (!this.pondHost?.nativeElement) {
      return;
    }

    const FilePond = await import('filepond');
    const { default: FilePondPluginFileValidateType } = await import('filepond-plugin-file-validate-type');
    const { default: FilePondPluginImageExifOrientation } = await import('filepond-plugin-image-exif-orientation');
    const { default: FilePondPluginImagePreview } = await import('filepond-plugin-image-preview');

    FilePond.registerPlugin(
      FilePondPluginFileValidateType,
      FilePondPluginImageExifOrientation,
      FilePondPluginImagePreview,
    );

    this.pond = FilePond.create(this.pondHost.nativeElement, {
      allowMultiple: true,
      allowReorder: true,
      allowDrop: true,
      allowPaste: true,
      credits: false,
      disabled: this.disabled,
      imagePreviewHeight: 120,
      itemInsertLocation: 'after',
      labelIdle: 'Drag images here or click to browse',
      acceptedFileTypes: ['image/*'],
      fileValidateTypeDetectType: (source, type) => this.detectFileType(source, type),
      onupdatefiles: (items: FilePondFile[]) => {
        const staged = items
          .map((item) => item.file as Blob & { name?: string })
          .filter((file): file is Blob & { name?: string } => Boolean(file));
        this.stagedFileNames = staged.map((file) => file.name ?? 'image');
        this.stagedFilesChange.emit(staged);
      },
    });
  }

  private detectFileType(source: File | Blob | string, type: string): Promise<string> {
    if (type && type !== 'application/octet-stream') {
      return Promise.resolve(type);
    }

    const fileName = typeof source === 'string' ? source : 'name' in source ? source.name ?? '' : '';
    const lowerName = fileName.toLowerCase();

    if (lowerName.endsWith('.jpg') || lowerName.endsWith('.jpeg') || lowerName.endsWith('.jfif')) {
      return Promise.resolve('image/jpeg');
    }
    if (lowerName.endsWith('.png')) {
      return Promise.resolve('image/png');
    }
    if (lowerName.endsWith('.webp')) {
      return Promise.resolve('image/webp');
    }
    if (lowerName.endsWith('.gif')) {
      return Promise.resolve('image/gif');
    }
    if (lowerName.endsWith('.avif')) {
      return Promise.resolve('image/avif');
    }
    if (lowerName.endsWith('.bmp')) {
      return Promise.resolve('image/bmp');
    }
    if (lowerName.endsWith('.tif') || lowerName.endsWith('.tiff')) {
      return Promise.resolve('image/tiff');
    }

    return Promise.resolve(type || 'image/jpeg');
  }

  ngOnDestroy(): void {
    if (this.pond && typeof (this.pond as { destroy?: () => void }).destroy === 'function') {
      (this.pond as { destroy: () => void }).destroy();
    }
  }
}
