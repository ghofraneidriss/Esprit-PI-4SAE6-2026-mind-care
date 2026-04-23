import { inject } from '@angular/core';
import { CanMatchFn, Router, UrlSegment } from '@angular/router';
import { AuthService } from '../frontoffice/auth/auth.service';

export const frontofficeAccessGuard: CanMatchFn = (_route, _segments: UrlSegment[]) => {
  return true;
};
