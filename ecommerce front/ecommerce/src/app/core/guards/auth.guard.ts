import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { SessionService } from '../services/session.service';

export const authGuard: CanActivateFn = () => {
  const session = inject(SessionService);
  const router = inject(Router);

  if (session.isAuthenticated()) {
    return true;
  }

  return router.createUrlTree(['/auth/login']);
};

export const guestGuard: CanActivateFn = () => {
  const session = inject(SessionService);
  const router = inject(Router);

  if (!session.isAuthenticated()) {
    return true;
  }

  return router.createUrlTree(['/']);
};

export function roleGuard(requiredRoles: string[]): CanActivateFn {
  return () => {
    const session = inject(SessionService);
    const router = inject(Router);

    if (!session.isAuthenticated()) {
      return router.createUrlTree(['/auth/login']);
    }

    if (session.hasAnyRole(requiredRoles)) {
      return true;
    }

    return router.createUrlTree(['/']);
  };
}
