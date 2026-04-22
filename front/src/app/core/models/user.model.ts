export type UserRole = 'ADMIN' | 'PATIENT' | 'DOCTOR' | 'CAREGIVER' | 'VOLUNTEER';

/** Réponse légère GET /users/patients (inscription aidant / bénévole). */
export interface PatientRegistrationOption {
  userId: number;
  firstName: string;
  lastName: string;
  email: string;
}

export interface User {
  userId: number;
  firstName: string;
  lastName: string;
  email: string;
  phone?: string;
  role: UserRole;
  /** For PATIENT: linked caregiver user id */
  caregiverId?: number | null;
  /** For PATIENT: linked volunteer user id */
  volunteerId?: number | null;
}
