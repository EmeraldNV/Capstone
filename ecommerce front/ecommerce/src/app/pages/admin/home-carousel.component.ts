import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { HomeCarouselImageResponse } from '../../core/models/home-carousel.models';
import { HomeCarouselApiService } from '../../core/services/home-carousel-api.service';
import { summarizeHttpError } from '../../core/utils/http-errors';
import { HomeCarouselManagerComponent } from './home-carousel-manager.component';

@Component({
  selector: 'app-home-carousel-page',
  standalone: true,
  imports: [CommonModule, RouterLink, HomeCarouselManagerComponent],
  templateUrl: './home-carousel.component.html',
  styleUrl: './home-carousel.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HomeCarouselComponent implements OnInit {
  private readonly api = inject(HomeCarouselApiService);

  protected loading = false;
  protected saving = false;
  protected uploading = false;
  protected message = '';
  protected errorMessage = '';
  protected errorDetails: string[] = [];
  protected images: HomeCarouselImageResponse[] = [];
  protected selectedFiles: Array<Blob & { name?: string }> = [];
  protected stagedResetToken = 0;

  ngOnInit(): void {
    this.loadImages();
  }

  submit(): void {
    if (!this.selectedFiles.length) {
      this.errorMessage = 'Seleziona almeno una immagine prima del caricamento.';
      this.errorDetails = [];
      return;
    }

    this.saving = true;
    this.errorMessage = '';
    this.errorDetails = [];
    this.message = '';

    this.api.uploadImages(this.selectedFiles).subscribe({
      next: (images) => {
        this.saving = false;
        this.images = images;
        this.selectedFiles = [];
        this.resetStagedFiles();
        this.message = 'Immagini del carousel aggiornate.';
      },
      error: (error) => {
        this.saving = false;
        const summary = summarizeHttpError(error, 'Caricamento immagini carousel fallito.');
        this.errorMessage = summary.message;
        this.errorDetails = summary.details;
      },
    });
  }

  onStagedFilesChanged(files: Array<Blob & { name?: string }>): void {
    this.selectedFiles = files;
  }

  removeImage(imageId: number): void {
    if (!window.confirm('Rimuovere questa immagine dal carousel home?')) {
      return;
    }
    this.uploading = true;
    this.api.deleteImage(imageId).subscribe({
      next: (images) => {
        this.uploading = false;
        this.images = images;
        this.message = 'Immagine rimossa.';
      },
      error: (error) => {
        this.uploading = false;
        const summary = summarizeHttpError(error, 'Rimozione immagine fallita.');
        this.errorMessage = summary.message;
        this.errorDetails = summary.details;
      },
    });
  }

  saveImageOrder(imageIds: number[]): void {
    this.uploading = true;
    this.api.reorderImages(imageIds).subscribe({
      next: (images) => {
        this.uploading = false;
        this.images = images;
        this.message = 'Ordine immagini aggiornato.';
      },
      error: (error) => {
        this.uploading = false;
        const summary = summarizeHttpError(error, 'Riordino immagini fallito.');
        this.errorMessage = summary.message;
        this.errorDetails = summary.details;
      },
    });
  }

  private loadImages(): void {
    this.loading = true;
    this.api.loadAdminCarousel().subscribe({
      next: (images) => {
        this.loading = false;
        this.images = images;
      },
      error: (error) => {
        this.loading = false;
        const summary = summarizeHttpError(error, 'Caricamento carousel fallito.');
        this.errorMessage = summary.message;
        this.errorDetails = summary.details;
      },
    });
  }

  private resetStagedFiles(): void {
    this.stagedResetToken++;
  }
}
