import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../frontoffice/auth/auth.service';

export const strictAdminGuard: CanActivateFn = () => {
    const authService = inject(AuthService);
    const router = inject(Router);
    const loggedUser = authService.getLoggedUser();

    if (!loggedUser) {
        return router.createUrlTree(['/auth/login']);
    }

    if (authService.isAdmin()) {
        return true;
    }

    return router.createUrlTree(['/admin']);
};
