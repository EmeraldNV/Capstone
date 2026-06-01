import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class ShopNavigationService {
  private readonly refreshTokenSubject = new BehaviorSubject<number>(0);

  readonly refreshToken$ = this.refreshTokenSubject.asObservable();

  triggerRefresh(token = Date.now()): void {
    this.refreshTokenSubject.next(token);
  }
}
