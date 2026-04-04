import { Injectable } from '@angular/core';
import {
  HttpRequest,
  HttpHandler,
  HttpEvent,
  HttpInterceptor,
} from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthService } from '../../frontoffice/auth/auth.service';

/**
 * Injects X-User-Id and X-User-Role headers into every outgoing HTTP request
 * so that backend services can scope responses to the logged-in user.
 */
@Injectable()
export class AuthHeadersInterceptor implements HttpInterceptor {
  constructor(private readonly authService: AuthService) {}

  intercept(req: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    const user = this.authService.getLoggedUser();
    if (!user) {
      return next.handle(req);
    }

    const cloned = req.clone({
      setHeaders: {
        'X-User-Id':   String(user.userId),
        'X-User-Role': user.role ?? '',
      },
    });

    return next.handle(cloned);
  }
}
