import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../auth/auth.service';
import { PatientProfileService, PatientProfile as ProfileModel } from './patient-profile.service';

@Component({
  selector: 'app-patient-profile',
  standalone: false,
  templateUrl: './patient-profile.html',
  styleUrl: './patient-profile.css',
})
export class PatientProfile implements OnInit {
  profile: ProfileModel = {
    userId: 0,
    email: '',
    dateOfBirth: '',
    bloodGroup: '',
    heightCm: null as any,
    weightKg: null as any,
    educationLevel: '',
    caregiverEmergencyNumber: '',
    isSmoker: false,
    drinksAlcohol: false,
    physicalActivity: false,
    familyHistoryAlzheimer: false,
    hypertension: false,
    type2Diabetes: false,
    hypercholesterolemia: false,
    sleepDisorders: false,
    medications: '',
    amedicaments: [],
    allergies: [],
  };

  isExistingProfile = false;
  isViewMode = false;
  isLoading = true;
  isSaving = false;
  errorMessage = '';
  successMessage = '';
  accountUser: any = null;

  constructor(
    private authService: AuthService,
    private profileService: PatientProfileService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) { }

  ngOnInit(): void {
    try {
      const user = this.authService.getLoggedUser();
      this.accountUser = user;

      if (!user) {
        this.router.navigate(['/auth/login']);
        return;
      }

      this.profile.userId = user.userId || 0;
      this.profile.email = user.email || '';

      if (!this.profile.email) {
        this.errorMessage = 'Email identifier not found in session. Please log in again.';
        this.isLoading = false;
        return;
      }

      this.profileService.getProfileByEmail(this.profile.email).subscribe({
        next: (profileData) => {
          if (profileData && profileData.id) {
            this.profile = profileData;
            this.isExistingProfile = true;
            this.isViewMode = true;
          } else {
            this.isExistingProfile = false;
            this.isViewMode = false;
          }
          this.isLoading = false;
          this.cdr.detectChanges(); // Force la mise à jour de la vue au premier chargement
        },
        error: (err) => {
          if (err.status !== 404) {
            this.errorMessage = 'Connection error to the profile server. Please try again.';
          } else {
            this.isExistingProfile = false;
            this.isViewMode = false;
          }
          this.isLoading = false;
          this.cdr.detectChanges(); // Force la mise à jour même en cas d'erreur
        }
      });
    } catch (error) {
      this.errorMessage = 'An internal error occurred.';
      this.isLoading = false;
    }
  }

  saveProfile(): void {
    this.isSaving = true;
    this.errorMessage = '';
    this.successMessage = '';

    if (this.profile.dateOfBirth) {
      const selectedDate = new Date(this.profile.dateOfBirth);
      const maxDateObj = new Date(this.maxAllowedDate);
      if (selectedDate > maxDateObj) {
        this.errorMessage = 'You must be at least 15 years old to register or update your medical profile.';
        this.isSaving = false;
        return;
      }
    }

    if (this.isExistingProfile && this.profile.id) {
      this.profileService.updateProfile(this.profile.id, this.profile).subscribe({
        next: (savedProfile) => {
          this.profile = savedProfile;
          this.isViewMode = true;
          this.isExistingProfile = true;
          this.successMessage = 'Profile updated successfully!';
          this.isSaving = false;
          window.scrollTo(0, 0);
        },
        error: (err) => {
          this.errorMessage = err.error?.message || 'Error updating profile. Please try again.';
          this.isSaving = false;
        }
      });
    } else {
      this.profileService.createProfile(this.profile).subscribe({
        next: (savedProfile) => {
          this.profile = savedProfile;
          this.isExistingProfile = true;
          this.isViewMode = true;
          this.successMessage = 'Your profile has been created successfully!';
          this.isSaving = false;
          window.scrollTo(0, 0);
        },
        error: (err) => {
          if (err.status === 409) {
            this.profileService.getProfileByEmail(this.profile.email).subscribe({
              next: (existing) => {
                this.profile = existing;
                this.isExistingProfile = true;
                this.isViewMode = true;
                this.isSaving = false;
              },
              error: () => {
                this.errorMessage = 'Profile already exists. Please refresh.';
                this.isSaving = false;
              }
            });
          } else {
            this.errorMessage = err.error?.message || 'Error creating profile.';
            this.isSaving = false;
          }
        }
      });
    }
  }


  deleteProfile(): void {
    if (!this.profile.id) return;
    if (confirm('Are you sure you want to delete your medical profile? This action cannot be undone.')) {
      this.isLoading = true;
      this.profileService.deleteProfile(this.profile.id).subscribe({
        next: () => {
          this.isExistingProfile = false;
          this.isViewMode = false; // Redirect back to form

          // Reset the form details
          const userId = this.profile.userId;
          const email = this.profile.email;
          this.profile = {
            userId: userId,
            email: email,
            dateOfBirth: '',
            bloodGroup: '',
            heightCm: null as any,
            weightKg: null as any,
            educationLevel: '',
            caregiverEmergencyNumber: '',
            isSmoker: false,
            drinksAlcohol: false,
            physicalActivity: false,
            familyHistoryAlzheimer: false,
            hypertension: false,
            type2Diabetes: false,
            hypercholesterolemia: false,
            sleepDisorders: false,
            medications: '',
            amedicaments: [],
            allergies: [],
          };
          this.successMessage = 'Profile deleted successfully. You can now create a new one.';
          this.isLoading = false;
          this.cdr.detectChanges();
          window.scrollTo(0, 0);
        },
        error: (err) => {
          console.error('[PatientProfile] Delete error:', err);
          this.errorMessage = 'Failed to delete profile.';
          this.isLoading = false;
          this.cdr.detectChanges();
        }
      });
    }
  }

  enableEditMode(): void {
    this.isViewMode = false;
    this.successMessage = '';
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/']);
  }

  viewMyPrescriptions(): void {
    if (this.profile.userId) {
      this.router.navigate(['/patient-prescriptions', this.profile.userId]);
    }
  }

  get patientAge(): number | null {
    if (!this.profile.dateOfBirth) return null;
    const dob = new Date(this.profile.dateOfBirth);
    if (isNaN(dob.getTime())) return null;
    const diffMs = Date.now() - dob.getTime();
    const ageDt = new Date(diffMs);
    return Math.abs(ageDt.getUTCFullYear() - 1970);
  }

  get maxAllowedDate(): string {
    const d = new Date();
    d.setFullYear(d.getFullYear() - 15);
    return d.toISOString().split('T')[0];
  }

  // --- Autocomplete & Tag Logic ---
  newAllergy: string = '';
  newMedication: string = '';
  allergySuggestions: string[] = [];
  medicationSuggestions: string[] = [];

  onAllergyInput(): void {
    console.log('[PatientProfile] Allergy input:', this.newAllergy);
    if (this.newAllergy.length > 0) {
      this.profileService.suggestAllergyCategories(this.newAllergy).subscribe(res => {
        console.log('[PatientProfile] Allergy result:', res);
        this.allergySuggestions = res;
      });
    } else {
      this.allergySuggestions = [];
    }
  }

  addAllergy(val?: string): void {
    const allergy = (val || this.newAllergy).trim();
    if (allergy && !this.profile.allergies?.includes(allergy)) {
      if (!this.profile.allergies) this.profile.allergies = [];
      this.profile.allergies.push(allergy);
    }
    this.newAllergy = '';
    this.allergySuggestions = [];
  }

  removeAllergy(index: number): void {
    this.profile.allergies?.splice(index, 1);
  }

  onMedicationInput(): void {
    console.log('[PatientProfile] Medication input:', this.newMedication);
    if (this.newMedication.length > 0) {
      this.profileService.suggestMedicationNames(this.newMedication).subscribe(res => {
        console.log('[PatientProfile] Medication result:', res);
        this.medicationSuggestions = res;
      });
    } else {
      this.medicationSuggestions = [];
    }
  }

  addMedication(val?: string): void {
    const med = (val || this.newMedication).trim();
    if (med && !this.profile.amedicaments?.includes(med)) {
      if (!this.profile.amedicaments) this.profile.amedicaments = [];
      this.profile.amedicaments.push(med);
    }
    this.newMedication = '';
    this.medicationSuggestions = [];
  }

  removeMedication(index: number): void {
    this.profile.amedicaments?.splice(index, 1);
  }
}
