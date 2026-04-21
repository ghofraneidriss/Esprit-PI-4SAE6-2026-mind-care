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
    console.log('[PatientProfile] ngOnInit started');

    // Failsafe: if loading takes too long, stop it so user can at least see something
    setTimeout(() => {
      if (this.isLoading) {
        console.warn('[PatientProfile] Loading took too long, forcing stop.');
        this.isLoading = false;
        this.cdr.detectChanges();
        if (!this.profile.email) {
          this.errorMessage = "Loading took too long. Please check that your session is still active.";
        }
      }
    }, 6000);

    try {
      const user = this.authService.getLoggedUser();
      console.log('[PatientProfile] Logged user data:', user);
      this.accountUser = user;

      if (!user) {
        console.warn('[PatientProfile] No user found, navigating to login');
        this.router.navigate(['/auth/login']);
        return;
      }

      this.profile.userId = user.userId || 0;
      this.profile.email = user.email || '';

      if (!this.profile.email) {
        console.error('[PatientProfile] User object is missing email attribute!');
        this.errorMessage = 'Email identifier not found in session. Please log in again.';
        this.isLoading = false;
        return;
      }

      console.log(`[PatientProfile] Fetching profile for email: ${this.profile.email}`);
      this.profileService.getProfileByEmail(this.profile.email).subscribe({
        next: (profileData) => {
          console.log('[PatientProfile] Profile fetched successfully:', profileData);
          if (profileData && profileData.id) {
            this.profile = profileData;
            this.isExistingProfile = true;
            this.isViewMode = true;
          } else {
            this.isExistingProfile = false;
            this.isViewMode = false;
          }
          this.isLoading = false;
          this.cdr.detectChanges();
        },
        error: (err) => {
          console.error('[PatientProfile] API Error when fetching profile:', err);
          // If profile is not found (404), that's fine, we create a new one
          if (err.status !== 404) {
            this.errorMessage = 'Connection error to the profile server. Please try again.';
          } else {
            console.log('[PatientProfile] No existing profile (404), starting fresh.');
            this.isExistingProfile = false;
            this.isViewMode = false;
          }
          this.isLoading = false;
          this.cdr.detectChanges();
        }
      });
    } catch (error) {
      console.error('[PatientProfile] Unexpected error in ngOnInit:', error);
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
        this.cdr.detectChanges();
        return;
      }
    }

    // Failsafe for save/update
    setTimeout(() => {
      if (this.isSaving) {
        console.warn('[PatientProfile] Save operation took too long, forcing spinner off.');
        this.isSaving = false;
        this.cdr.detectChanges();
        if (!this.successMessage) {
          this.errorMessage = 'The server is taking too long to respond. Please refresh the page to see if your changes were saved.';
        }
      }
    }, 8000);

    if (this.isExistingProfile && this.profile.id) {
      console.log('[PatientProfile] Updating existing profile id:', this.profile.id);
      this.profileService.updateProfile(this.profile.id, this.profile).subscribe({
        next: (savedProfile) => {
          console.log('[PatientProfile] Update successful from API:', savedProfile);
          this.profile = savedProfile;
          this.isViewMode = true;
          this.isExistingProfile = true;
          this.successMessage = 'Profile updated successfully!';
          this.isSaving = false;
          this.cdr.detectChanges();
          window.scrollTo(0, 0);
        },
        error: (err) => {
          console.error('[PatientProfile] Error updating profile details:', err);
          if (err.status === 404) {
            this.errorMessage = 'Profile not found on the server. Please refresh the page.';
          } else if (err.status === 0) {
            this.errorMessage = 'Cannot reach the server. Make sure the backend is running (port 8081).';
          } else if (err.error && err.error.message) {
            this.errorMessage = err.error.message;
          } else {
            this.errorMessage = `Error updating profile (${err.status}). Please try again.`;
          }
          this.isSaving = false;
          this.cdr.detectChanges();
        }
      });
    } else {
      console.log('[PatientProfile] Creating new profile with data:', this.profile);
      this.profileService.createProfile(this.profile).subscribe({
        next: (savedProfile) => {
          console.log('[PatientProfile] Profile created successfully:', savedProfile);
          this.profile = savedProfile;
          this.isExistingProfile = true;
          this.isViewMode = true;
          this.successMessage = 'Your profile has been created successfully!';
          this.isSaving = false;
          this.cdr.detectChanges();
          window.scrollTo(0, 0);
        },
        error: (err) => {
          console.error('[PatientProfile] Error creating profile:', err);
          if (err.status === 409) {
            // Profile already exists — reload it automatically
            this.errorMessage = 'You already have a medical profile. Loading your existing profile...';
            this.isSaving = false;
            this.cdr.detectChanges();
            // Re-fetch the existing profile to display it
            this.profileService.getProfileByEmail(this.profile.email).subscribe({
              next: (existing) => {
                if (existing && existing.id) {
                  this.profile = existing;
                  this.isExistingProfile = true;
                  this.isViewMode = true;
                  this.errorMessage = '';
                  this.successMessage = '';
                  this.cdr.detectChanges();
                  window.scrollTo(0, 0);
                }
              },
              error: () => {
                this.errorMessage = 'You already have a profile. Please refresh the page.';
                this.cdr.detectChanges();
              }
            });
          } else if (err.status === 0) {
            this.errorMessage = 'Cannot reach the server. Make sure the backend is running (port 8081).';
            this.isSaving = false;
            this.cdr.detectChanges();
          } else if (err.error && err.error.message) {
            this.errorMessage = err.error.message;
            this.isSaving = false;
            this.cdr.detectChanges();
          } else {
            this.errorMessage = `Error creating profile (${err.status}). Please ensure the server is running.`;
            this.isSaving = false;
            this.cdr.detectChanges();
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

  viewPrescriptions(): void {
    const patientId = this.profile.userId || this.accountUser?.userId || 0;
    if (patientId) {
      this.router.navigate(['/patient-prescriptions', patientId]);
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
}
