import { Component, OnInit } from '@angular/core';
import { AuthService } from '../../core/services/auth.service';
import { User } from '../../core/models/user.model';

@Component({
  selector: 'app-user-management',
  standalone: false,
  templateUrl: './user-management.html',
  styleUrls: ['./user-management.css'],
})
export class UserManagementPage implements OnInit {

  users: User[] = [];
  loading = false;
  error: string | null = null;
  successMessage: string | null = null;

  // Assignment modal
  isAssignModalOpen = false;
  selectedPatient: User | null = null;
  selectedCaregiverId: number | null = null;
  assigning = false;

  constructor(private authService: AuthService) {}

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.loading = true;
    this.error = null;
    this.authService.getAllUsers().subscribe({
      next: (data) => {
        this.users = data;
        this.loading = false;
      },
      error: () => {
        this.error = 'Failed to load users. Is users_service running?';
        this.loading = false;
      }
    });
  }

  get patients(): User[] {
    return this.users.filter(u => u.role === 'PATIENT');
  }

  get caregivers(): User[] {
    return this.users.filter(u => u.role === 'CAREGIVER');
  }

  getCaregiverName(caregiverId: number | null | undefined): string {
    if (!caregiverId) return '—';
    const cg = this.users.find(u => u.userId === caregiverId);
    return cg ? `${cg.firstName} ${cg.lastName}` : `#${caregiverId}`;
  }

  openAssignModal(patient: User): void {
    this.selectedPatient = patient;
    this.selectedCaregiverId = patient.caregiverId ?? null;
    this.isAssignModalOpen = true;
    this.successMessage = null;
  }

  saveAssignment(): void {
    if (!this.selectedPatient) return;
    this.assigning = true;

    const updated = {
      firstName: this.selectedPatient.firstName,
      lastName: this.selectedPatient.lastName,
      phone: this.selectedPatient.phone || '',
      role: this.selectedPatient.role,
      caregiverId: this.selectedCaregiverId
    };

    this.authService.updateUser(this.selectedPatient.userId, updated).subscribe({
      next: () => {
        this.assigning = false;
        this.isAssignModalOpen = false;
        this.successMessage = `${this.selectedPatient!.firstName} assigned successfully.`;
        this.loadUsers();
        setTimeout(() => this.successMessage = null, 3000);
      },
      error: () => {
        this.assigning = false;
        this.error = 'Failed to update assignment.';
      }
    });
  }

  getRoleBadgeClass(role: string): string {
    switch (role) {
      case 'ADMIN':     return 'bg-danger';
      case 'DOCTOR':    return 'bg-primary';
      case 'CAREGIVER': return 'bg-success';
      case 'PATIENT':   return 'bg-info text-dark';
      default:          return 'bg-secondary';
    }
  }
}
