import { Component, OnInit } from '@angular/core';
import {
  AuthService,
  AuthUser,
  RegisterRequest,
  UpdateUserRequest,
} from '../../frontoffice/auth/auth.service';

interface UserFormModel {
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  role: string;
  password: string;
}

@Component({
  selector: 'app-user-management',
  standalone: false,
  templateUrl: './user-management.html',
  styleUrls: ['./user-management.css'],
})
export class UserManagementPage implements OnInit {
  readonly roles = ['ADMIN', 'DOCTOR', 'CAREGIVER', 'PATIENT', 'VOLUNTEER'];

  users: AuthUser[] = [];
  filteredUsers: AuthUser[] = [];
  isLoading = true;
  isSubmitting = false;
  searchTerm = '';
  errorMessage = '';
  successMessage = '';
  editingUserId: number | null = null;

  createForm: UserFormModel = this.createEmptyForm();
  editForm: UserFormModel = this.createEmptyForm();

  constructor(private readonly authService: AuthService) {}

  ngOnInit(): void {
    this.fetchUsers();
  }

  fetchUsers(): void {
    this.isLoading = true;
    this.errorMessage = '';

    this.authService.getAllUsers().subscribe({
      next: (users) => {
        this.users = users;
        this.applyFilters();
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Failed to load users', err);
        this.errorMessage = this.buildHttpError('Failed to load users', err);
        this.isLoading = false;
      },
    });
  }

  applyFilters(): void {
    const term = this.searchTerm.trim().toLowerCase();

    if (!term) {
      this.filteredUsers = [...this.users];
      return;
    }

    this.filteredUsers = this.users.filter((user) => {
      const haystack = [
        user.userId,
        user.firstName,
        user.lastName,
        user.email,
        user.phone,
        user.role,
      ]
        .filter(Boolean)
        .join(' ')
        .toLowerCase();

      return haystack.includes(term);
    });
  }

  createUser(): void {
    if (!this.isCreateFormValid()) {
      this.errorMessage = 'Please fill in first name, last name, email, password, and role.';
      return;
    }

    this.isSubmitting = true;
    this.errorMessage = '';
    this.successMessage = '';

    const payload: RegisterRequest = {
      firstName: this.createForm.firstName.trim(),
      lastName: this.createForm.lastName.trim(),
      email: this.createForm.email.trim(),
      password: this.createForm.password,
      role: this.createForm.role,
      phone: this.normalizeOptionalValue(this.createForm.phone),
    };

    this.authService.register(payload).subscribe({
      next: () => {
        this.successMessage = 'User created successfully.';
        this.resetCreateForm();
        this.isSubmitting = false;
        this.fetchUsers();
      },
      error: (err) => {
        console.error('Failed to create user', err);
        this.errorMessage = this.buildHttpError('Failed to create user', err);
        this.isSubmitting = false;
      },
    });
  }

  startEdit(user: AuthUser): void {
    this.editingUserId = user.userId;
    this.editForm = {
      firstName: user.firstName ?? '',
      lastName: user.lastName ?? '',
      email: user.email ?? '',
      phone: user.phone ?? '',
      role: this.authService.normalizeRole(user.role),
      password: '',
    };
    this.successMessage = '';
    this.errorMessage = '';
  }

  cancelEdit(): void {
    this.editingUserId = null;
    this.editForm = this.createEmptyForm();
  }

  resetCreateForm(): void {
    this.createForm = this.createEmptyForm();
  }

  saveEdit(userId: number): void {
    if (!this.isEditFormValid()) {
      this.errorMessage = 'Please fill in first name, last name, and role before saving.';
      return;
    }

    this.isSubmitting = true;
    this.errorMessage = '';
    this.successMessage = '';

    const payload: UpdateUserRequest = {
      firstName: this.editForm.firstName.trim(),
      lastName: this.editForm.lastName.trim(),
      phone: this.normalizeOptionalValue(this.editForm.phone),
      role: this.editForm.role,
      password: this.normalizeOptionalValue(this.editForm.password),
    };

    this.authService.updateUser(userId, payload).subscribe({
      next: () => {
        this.successMessage = 'User updated successfully.';
        this.isSubmitting = false;
        this.cancelEdit();
        this.fetchUsers();
      },
      error: (err) => {
        console.error('Failed to update user', err);
        this.errorMessage = this.buildHttpError('Failed to update user', err);
        this.isSubmitting = false;
      },
    });
  }

  saveCurrentEdit(): void {
    if (this.editingUserId === null) {
      return;
    }

    this.saveEdit(this.editingUserId);
  }

  deleteUser(user: AuthUser): void {
    const fullName = `${user.firstName} ${user.lastName}`.trim();

    if (!confirm(`Delete ${fullName || 'this user'}?`)) {
      return;
    }

    this.errorMessage = '';
    this.successMessage = '';

    this.authService.deleteUser(user.userId).subscribe({
      next: () => {
        this.successMessage = 'User deleted successfully.';
        if (this.editingUserId === user.userId) {
          this.cancelEdit();
        }
        this.fetchUsers();
      },
      error: (err) => {
        console.error('Failed to delete user', err);
        this.errorMessage = this.buildHttpError('Failed to delete user', err);
      },
    });
  }

  getTotalUsers(): number {
    return this.users.length;
  }

  getRoleCount(role: string): number {
    return this.users.filter((user) => this.authService.normalizeRole(user.role) === role).length;
  }

  formatRole(role?: string): string {
    const normalized = this.authService.normalizeRole(role);
    return normalized ? normalized.charAt(0) + normalized.slice(1).toLowerCase() : 'Unknown';
  }

  formatDate(createdAt?: string): string {
    if (!createdAt) {
      return 'N/A';
    }

    const date = new Date(createdAt);
    if (Number.isNaN(date.getTime())) {
      return createdAt;
    }

    return new Intl.DateTimeFormat('en-GB', {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
    }).format(date);
  }

  private createEmptyForm(): UserFormModel {
    return {
      firstName: '',
      lastName: '',
      email: '',
      phone: '',
      role: 'PATIENT',
      password: '',
    };
  }

  private isCreateFormValid(): boolean {
    return Boolean(
      this.createForm.firstName.trim() &&
        this.createForm.lastName.trim() &&
        this.createForm.email.trim() &&
        this.createForm.password &&
        this.createForm.role,
    );
  }

  private isEditFormValid(): boolean {
    return Boolean(
      this.editForm.firstName.trim() && this.editForm.lastName.trim() && this.editForm.role,
    );
  }

  private normalizeOptionalValue(value: string): string | undefined {
    const trimmedValue = value.trim();
    return trimmedValue ? trimmedValue : undefined;
  }

  private buildHttpError(prefix: string, err: { error?: unknown; message?: string }): string {
    const apiMessage =
      typeof err.error === 'string'
        ? err.error
        : typeof err.error === 'object' &&
            err.error !== null &&
            'message' in err.error &&
            typeof (err.error as { message?: unknown }).message === 'string'
          ? (err.error as { message: string }).message
          : err.message;

    return apiMessage ? `${prefix}: ${apiMessage}` : prefix;
  }
}
