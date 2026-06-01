import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { BehaviorSubject, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { HomeCarouselImageResponse } from '../models/home-carousel.models';

@Injectable({
  providedIn: 'root',
})
export class HomeCarouselApiService {
  private readonly http = inject(HttpClient);
  private readonly publicBaseUrl = `${environment.apiBaseUrl}/home-carousel`;
  private readonly adminBaseUrl = `${environment.apiBaseUrl}/admin/home-carousel`;
  private readonly carouselSubject = new BehaviorSubject<HomeCarouselImageResponse[]>([]);

  readonly carousel$ = this.carouselSubject.asObservable();

  loadPublicCarousel() {
    return this.http.get<HomeCarouselImageResponse[]>(this.publicBaseUrl).pipe(
      tap((images) => this.carouselSubject.next(images)),
    );
  }

  loadAdminCarousel() {
    return this.http.get<HomeCarouselImageResponse[]>(this.adminBaseUrl).pipe(
      tap((images) => this.carouselSubject.next(images)),
    );
  }

  uploadImages(files: Array<Blob & { name?: string }>) {
    const formData = new FormData();
    for (const file of files) {
      formData.append('files', file, file.name ?? 'image');
    }
    return this.http.post<HomeCarouselImageResponse[]>(`${this.adminBaseUrl}/images`, formData).pipe(
      tap((images) => this.carouselSubject.next(images)),
    );
  }

  reorderImages(imageIds: number[]) {
    return this.http.put<HomeCarouselImageResponse[]>(`${this.adminBaseUrl}/images/order`, { imageIds }).pipe(
      tap((images) => this.carouselSubject.next(images)),
    );
  }

  deleteImage(imageId: number) {
    return this.http.delete<HomeCarouselImageResponse[]>(`${this.adminBaseUrl}/images/${imageId}`).pipe(
      tap((images) => this.carouselSubject.next(images)),
    );
  }
}
