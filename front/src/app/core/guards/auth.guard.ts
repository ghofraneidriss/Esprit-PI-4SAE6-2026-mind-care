import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isLoggedIn()) return true;

  router.navigate(['/login-cover']);
  return false;
};

export const adminGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (!authService.isLoggedIn()) {
    router.navigate(['/login-cover']);
    return false;
  }

  const role = authService.getRole();
  if (role === 'ADMIN' || role === 'DOCTOR') return true;

  router.navigate(['/']);
  return false;
};

/**
 * Staff (admin/doctor): patient CVP area is `/officiel/*` — they must use `/admin` only.
 * (Patients and caregivers: unchanged access.)
 */
export const redirectStaffFromOfficielGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isLoggedIn() && authService.hasStaffDashboardAccess()) {
    router.navigate(['/admin']);
    return false;
  }
  return true;
};

/** Public home: redirect admin and doctor to the back office. */
export const redirectStaffFromPublicHomeGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.hasStaffDashboardAccess()) {
    router.navigate(['/admin']);
    return false;
  }
  return true;
};

/** @deprecated Use `redirectStaffFromPublicHomeGuard`. */
export const redirectDoctorFromPublicHomeGuard = redirectStaffFromPublicHomeGuard;

/** Incident report: caregivers and volunteers only (not patients). */
export const incidentReportGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const r = authService.getRole();
  if (r === 'CAREGIVER' || r === 'VOLUNTEER') return true;
  router.navigate(['/incidents/history'], { replaceUrl: true });
  return false;
};

/** Safe zones: admin, doctor, caregiver, volunteer (linked to a patient). */
export const safeZoneManagerGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (!auth.isLoggedIn()) {
    router.navigate(['/login-cover']);
    return false;
  }
  const r = auth.getRole();
  if (r === 'ADMIN' || r === 'DOCTOR' || r === 'CAREGIVER' || r === 'VOLUNTEER') {
    return true;
  }
  router.navigate(['/']);
  return false;
};

/** Location / movement: admin, doctor, patient, caregiver, volunteer. */
export const localizationFeatureGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (!auth.isLoggedIn()) {
    router.navigate(['/login-cover']);
    return false;
  }
  const r = auth.getRole();
  if (r === 'ADMIN' || r === 'DOCTOR' || r === 'PATIENT' || r === 'CAREGIVER' || r === 'VOLUNTEER') {
    return true;
  }
  router.navigate(['/']);
  return false;
};
