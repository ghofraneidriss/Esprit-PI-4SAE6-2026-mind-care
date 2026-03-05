import { Component, OnInit } from '@angular/core';
import { AuthService, AuthUser } from '../../frontoffice/auth/auth.service';

@Component({
  selector: 'app-user-management',
  standalone: false,
  templateUrl: './user-management.html',
  styleUrls: ['./user-management.css'],
})
export class UserManagementPage implements OnInit {
  users: AuthUser[] = [];
  isLoading = true;
  errorMessage = '';
  roles = ['ADMIN', 'DOCTOR', 'CAREGIVER', 'PATIENT'];

  constructor(private readonly authService: AuthService) { }

  ngOnInit(): void {
    this.fetchUsers();
  }

  fetchUsers(): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.authService.getAllUsers().subscribe({
      next: (data) => {
        this.users = data;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Failed to load users', err);
        this.errorMessage = `Failed to load users: ${err.status} ${err.statusText || err.message || 'Connection refused'}`;
        this.isLoading = false;
      }
    });
  }

  deleteUser(id: number): void {
    if (confirm('Are you sure you want to delete this user?')) {
      this.authService.deleteUser(id).subscribe({
        next: () => {
          this.fetchUsers();
        },
        error: (err) => {
          console.error('Failed to delete user', err);
        }
      });
    }
  }

  updateRole(user: AuthUser, event: Event): void {
    const select = event.target as HTMLSelectElement;
    const newRole = select.value;

    this.authService.updateUser(user.userId, {
      firstName: user.firstName,
      lastName: user.lastName,
      role: newRole
    }).subscribe({
      next: () => {
        this.fetchUsers();
      },
      error: (err) => {
        console.error('Failed to update role', err);
        alert('Failed to update user role');
      }
    });
  }
}
