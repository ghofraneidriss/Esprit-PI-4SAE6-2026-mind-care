import { inject } from '@angular/core';
import { CanMatchFn, Router, UrlSegment } from '@angular/router';
import { AuthService } from '../frontoffice/auth/auth.service';

export const frontofficeAccessGuard: CanMatchFn = (_route, _segments: UrlSegment[]) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // Permettre à tout le monde d'accéder aux routes d'authentification sans être bloqué
  if (_segments.length > 0 && _segments[0].path === 'auth') {
    return true;
  }

  if (authService.isBackofficeRole()) {
    return router.createUrlTree(['/admin']);
  }

  return true;
};
