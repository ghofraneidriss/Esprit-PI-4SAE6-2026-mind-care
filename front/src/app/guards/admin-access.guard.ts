import { inject } from '@angular/core';
import { CanMatchFn, Router, UrlSegment } from '@angular/router';
import { AuthService } from '../frontoffice/auth/auth.service';

export const adminAccessGuard: CanMatchFn = (_route, _segments: UrlSegment[]) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const loggedUser = authService.getLoggedUser();

  if (!loggedUser) {
    return router.createUrlTree(['/auth/login']);
  }

  if (authService.isBackofficeRole(loggedUser.role)) {
    return true;
  }

  return router.createUrlTree(['/']);
};
