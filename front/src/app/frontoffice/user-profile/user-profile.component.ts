import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { User, UserRole } from '../../core/models/user.model';

@Component({
    selector: 'app-user-profile',
    standalone: true,
    imports: [CommonModule, RouterModule, ReactiveFormsModule],
    templateUrl: './user-profile.component.html',
    styleUrl: './user-profile.component.css'
})
export class UserProfileComponent implements OnInit {
    form!: FormGroup;
    /** True while saving profile (PUT). */
    loading = false;
    /** True while fetching fresh profile from API (GET). */
    profileRefreshing = false;
    initialLoad = true;
    loadError = '';
    saveError = '';
    successMsg = '';

    constructor(
        public authService: AuthService,
        private fb: FormBuilder,
        private cdr: ChangeDetectorRef
    ) {
        this.form = this.fb.group({
            firstName: ['', [Validators.required, Validators.minLength(2)]],
            lastName: ['', [Validators.required, Validators.minLength(2)]],
            email: ['', [Validators.required, Validators.email]],
            phone: [''],
            newPassword: [''],
            confirmPassword: ['']
        });
    }

    ngOnInit(): void {
        const id = this.authService.getUserId();
        if (!id) {
            this.loadError = 'Not signed in.';
            this.initialLoad = false;
            return;
        }

        const cached = this.authService.getCurrentUser();
        if (cached) {
            this.patchForm(cached);
            this.initialLoad = false;
            this.cdr.detectChanges();
        }

        this.profileRefreshing = true;
        this.authService.getUserById(id).pipe(
            finalize(() => {
                this.profileRefreshing = false;
                this.initialLoad = false;
                this.cdr.detectChanges();
            })
        ).subscribe({
            next: (u) => {
                this.patchForm(u);
                this.authService.setCurrentUser(u);
            },
            error: () => {
                if (cached) {
                    return;
                }
                const u = this.authService.getCurrentUser();
                if (u) {
                    this.patchForm(u);
                } else {
                    this.loadError = 'Could not load profile from server.';
                }
            }
        });
    }

    private patchForm(u: User): void {
        this.form.patchValue({
            firstName: u.firstName || '',
            lastName: u.lastName || '',
            email: u.email || '',
            phone: u.phone || ''
        });
    }

    onSubmit(): void {
        this.saveError = '';
        this.successMsg = '';
        const id = this.authService.getUserId();
        const session = this.authService.getCurrentUser();
        if (!id || !session) {
            return;
        }

        if (this.form.invalid) {
            this.form.markAllAsTouched();
            return;
        }

        const v = this.form.value;
        const np = (v.newPassword || '').trim();
        const cp = (v.confirmPassword || '').trim();
        if (np || cp) {
            if (np.length < 6) {
                this.saveError = 'New password must be at least 6 characters.';
                return;
            }
            if (np !== cp) {
                this.saveError = 'Password confirmation does not match.';
                return;
            }
        }

        const payload: Partial<User> & { password?: string } = {
            userId: id,
            email: (v.email || '').trim(),
            role: session.role,
            caregiverId: session.caregiverId ?? undefined,
            firstName: v.firstName.trim(),
            lastName: v.lastName.trim(),
            phone: v.phone?.trim() || undefined
        };
        if (np) {
            payload.password = np;
        }

        this.loading = true;
        this.authService.updateUser(id, payload).pipe(
            finalize(() => {
                this.loading = false;
                this.cdr.detectChanges();
            })
        ).subscribe({
            next: (updated) => {
                this.authService.setCurrentUser(updated);
                this.successMsg = 'Profile updated successfully.';
                this.form.patchValue({ newPassword: '', confirmPassword: '' });
            },
            error: (err) => {
                this.saveError = err?.error?.message || 'Update failed. Try again.';
            }
        });
    }

    get initials(): string {
        const u = this.authService.getCurrentUser();
        if (!u) return '?';
        const a = (u.firstName || '?').charAt(0);
        const b = (u.lastName || '').charAt(0);
        return (a + b).toUpperCase();
    }

    displayName(u: User): string {
        const fn = (u.firstName || '').trim();
        const ln = (u.lastName || '').trim();
        if (fn || ln) {
            return `${fn} ${ln}`.trim();
        }
        return u.email || 'User';
    }

    roleLabel(role: UserRole | undefined): string {
        if (!role) return '';
        const labels: Record<UserRole, string> = {
            ADMIN: 'Administrator',
            PATIENT: 'Patient',
            DOCTOR: 'Doctor',
            CAREGIVER: 'Caregiver',
            VOLUNTEER: 'Volunteer'
        };
        return labels[role] ?? role;
    }
}
