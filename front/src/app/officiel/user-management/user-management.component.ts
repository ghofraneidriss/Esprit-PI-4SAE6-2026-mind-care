import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { QuizLimitService, QuizLimitStatus } from '../../core/services/quiz-limit.service';
import { AuthService } from '../../core/services/auth.service';
import { PatientMovementService } from '../../core/services/patient-movement.service';
import { environment } from '../../../environments/environment';
import type { UserRole } from '../../core/models/user.model';
import { ToastService } from '../../core/services/toast.service';
import { ConfirmDialogService } from '../../core/services/confirm-dialog.service';
import {
  CLINICAL_MOVEMENT_ALERTS_CRITICAL_THRESHOLD,
  isCriticalClinicalMovementAlertCount
} from '../../core/constants/critical-care.constants';
import {
  ADMIN_TABLE_PAGE_SIZE,
  padPageRows,
  slicePage,
  totalPageCount
} from '../../core/utils/admin-table-paging';

@Component({
  selector: 'app-off-user-mgmt',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './user-management.component.html',
  styleUrls: ['../quiz-management/quiz-management.component.css', '../mgmt-shared.css', './user-management.component.css']
})
export class UserManagementComponent implements OnInit {
  readonly clinicalMovementAlertThreshold = CLINICAL_MOVEMENT_ALERTS_CRITICAL_THRESHOLD;

  users: any[] = [];
  /** patientId → count of OUT_OF_SAFE_ZONE alerts (movement service). */
  safeZoneExitCountByPatientId: Record<number, number> = {};
  movementAlertsLoaded = false;
  loading = true;
  showForm = false;
  editing = false;
  private readonly api = `${environment.apiUrl}/users`;

  filterSearch = '';
  filterRole: '' | 'PATIENT' | 'DOCTOR' | 'CAREGIVER' | 'ADMIN' = '';
  /** When on, list only patients with critical safe-zone exit count (same rule as Critical patients page). */
  filterCriticalPatientsOnly = false;
  filterDateFrom = '';
  filterDateTo = '';

  pageIndex = 0;
  readonly pageSize = ADMIN_TABLE_PAGE_SIZE;

  selectedIds: number[] = [];

  form: any = this.emptyForm();

  // Quiz limit state
  showLimitModal = false;
  limitPatient: any = null;
  limitStatus: QuizLimitStatus | null = null;
  limitValue: number = 10;
  limitLoading = false;
  limitSaving = false;

  constructor(
    private http: HttpClient,
    private cdr: ChangeDetectorRef,
    private quizLimitSvc: QuizLimitService,
    private auth: AuthService,
    private toast: ToastService,
    private confirm: ConfirmDialogService,
    private movement: PatientMovementService
  ) {}

  ngOnInit() {
    this.load();
  }

  emptyForm() {
    return { id: null, firstname: '', lastname: '', email: '', password: '', phone: '', role: 'PATIENT' };
  }

  get filteredUsers(): any[] {
    let list = [...this.users];
    const q = (this.filterSearch || '').trim().toLowerCase();
    if (q) {
      list = list.filter(
        (u) =>
          (u.firstname || '').toLowerCase().includes(q) ||
          (u.lastname || '').toLowerCase().includes(q) ||
          (u.email || '').toLowerCase().includes(q)
      );
    }
    if (this.filterRole) {
      list = list.filter((u) => String(u.role || '').toUpperCase() === this.filterRole);
    }
    if (this.filterDateFrom) {
      const from = new Date(this.filterDateFrom);
      from.setHours(0, 0, 0, 0);
      list = list.filter((u) => {
        if (!u.createdAt) return false;
        return new Date(u.createdAt) >= from;
      });
    }
    if (this.filterDateTo) {
      const to = new Date(this.filterDateTo);
      to.setHours(23, 59, 59, 999);
      list = list.filter((u) => {
        if (!u.createdAt) return false;
        return new Date(u.createdAt) <= to;
      });
    }
    if (this.filterCriticalPatientsOnly) {
      list = list.filter((u) => {
        if (String(u.role || '').toUpperCase() !== 'PATIENT') return false;
        const n = this.safeZoneExitCountByPatientId[Number(u.id)] ?? 0;
        return isCriticalClinicalMovementAlertCount(n);
      });
    }
    return list;
  }

  get pagedFilteredUsers(): any[] {
    return slicePage(this.filteredUsers, this.pageIndex, this.pageSize);
  }

  get userTableRows(): (any | null)[] {
    if (this.loading || !this.filteredUsers.length) return [];
    return padPageRows(this.pagedFilteredUsers, this.pageSize);
  }

  get userTotalPages(): number {
    return totalPageCount(this.filteredUsers.length, this.pageSize);
  }

  get userRangeLabel(): string {
    const n = this.filteredUsers.length;
    if (!n) return '';
    const start = this.pageIndex * this.pageSize + 1;
    const end = Math.min(n, (this.pageIndex + 1) * this.pageSize);
    return `${start}–${end} of ${n}`;
  }

  userFiltersChanged() {
    this.pageIndex = 0;
  }

  userPrevPage() {
    if (this.pageIndex > 0) this.pageIndex--;
  }

  userNextPage() {
    if (this.pageIndex < this.userTotalPages - 1) this.pageIndex++;
  }

  get allFilteredSelected(): boolean {
    const f = this.pagedFilteredUsers;
    return f.length > 0 && f.every((u) => this.selectedIds.includes(u.id));
  }

  isSelected(id: number): boolean {
    return this.selectedIds.includes(id);
  }

  toggleRow(id: number, checked: boolean) {
    if (checked) {
      if (!this.selectedIds.includes(id)) this.selectedIds = [...this.selectedIds, id];
    } else {
      this.selectedIds = this.selectedIds.filter((x) => x !== id);
    }
  }

  toggleSelectAll(checked: boolean) {
    const ids = this.pagedFilteredUsers.map((u) => u.id);
    if (checked) {
      const set = new Set([...this.selectedIds, ...ids]);
      this.selectedIds = [...set];
    } else {
      const idSet = new Set(ids);
      this.selectedIds = this.selectedIds.filter((id) => !idSet.has(id));
    }
  }

  resetFilters() {
    this.filterSearch = '';
    this.filterRole = '';
    this.filterCriticalPatientsOnly = false;
    this.filterDateFrom = '';
    this.filterDateTo = '';
    this.selectedIds = [];
    this.pageIndex = 0;
  }

  safeZoneExitsForUser(u: { id: number; role?: string }): number | null {
    if (String(u.role || '').toUpperCase() !== 'PATIENT') return null;
    if (!this.movementAlertsLoaded) return null;
    return this.safeZoneExitCountByPatientId[Number(u.id)] ?? 0;
  }

  load() {
    this.loading = true;
    forkJoin({
      users: this.http.get<any[]>(this.api),
      counts: this.movement.getTotalMovementAlertCountsByPatient().pipe(catchError(() => of(null)))
    }).subscribe({
      next: ({ users, counts }) => {
        const map: Record<number, number> = {};
        if (counts && typeof counts === 'object') {
          for (const k of Object.keys(counts)) {
            map[Number(k)] = Number(counts[k]) || 0;
          }
          this.movementAlertsLoaded = true;
        } else {
          this.movementAlertsLoaded = false;
        }
        this.safeZoneExitCountByPatientId = map;

        this.users = (users || []).map((u) => ({
          ...u,
          id: u.userId ?? u.id,
          firstname: u.firstName ?? u.firstname,
          lastname: u.lastName ?? u.lastname
        }));
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (e) => {
        this.loading = false;
        this.toast.showHttpError(e, 'Could not load users. Please refresh the page.');
        this.cdr.detectChanges();
      }
    });
  }

  openCreate() {
    this.form = this.emptyForm();
    this.editing = false;
    this.showForm = true;
  }

  openEdit(u: any) {
    this.form = { ...u, password: '' };
    this.editing = true;
    this.showForm = true;
  }

  save() {
    if (this.editing) {
      const payload: Record<string, unknown> = {
        firstName: this.form.firstname,
        lastName: this.form.lastname,
        email: this.form.email,
        phone: this.form.phone,
        role: this.form.role
      };
      if (this.form.password) {
        payload['password'] = this.form.password;
      }
      this.auth.updateUser(this.form.id, payload as any).subscribe({
        next: () => {
          this.toast.show('User updated', 'success');
          this.showForm = false;
          this.load();
          this.cdr.detectChanges();
        },
        error: (e) => {
          this.toast.showHttpError(e, 'Could not update this user. Please try again.');
          this.cdr.detectChanges();
        }
      });
    } else {
      this.auth
        .register({
          firstName: this.form.firstname,
          lastName: this.form.lastname,
          email: this.form.email,
          password: this.form.password,
          phone: this.form.phone,
          role: this.form.role as UserRole
        })
        .subscribe({
          next: () => {
            this.toast.show('User created', 'success');
            this.showForm = false;
            this.load();
            this.cdr.detectChanges();
          },
          error: (e) => {
            this.toast.showHttpError(e, 'Could not create this user. Please check the details and try again.');
            this.cdr.detectChanges();
          }
        });
    }
  }

  async deleteOne(id: number) {
    const ok = await this.confirm.confirm({
      title: 'Delete user',
      message: 'Are you sure you want to delete this user? This cannot be undone.',
      confirmText: 'Delete',
      cancelText: 'Cancel',
      danger: true
    });
    if (!ok) return;
    this.http.delete(`${this.api}/${id}`).subscribe({
      next: () => {
        this.toast.show('User deleted', 'success');
        this.selectedIds = this.selectedIds.filter((x) => x !== id);
        this.load();
        this.cdr.detectChanges();
      },
      error: (e) => {
        this.toast.showHttpError(e, 'Could not delete this user. Please try again.');
        this.cdr.detectChanges();
      }
    });
  }

  async deleteSelected() {
    const ids = [...this.selectedIds];
    if (!ids.length) return;
    const ok = await this.confirm.confirm({
      title: 'Delete users',
      message: `Delete ${ids.length} user(s)? This cannot be undone.`,
      confirmText: 'Delete',
      cancelText: 'Cancel',
      danger: true
    });
    if (!ok) return;
    forkJoin(
      ids.map((id) =>
        this.http.delete(`${this.api}/${id}`).pipe(
          catchError(() => of({ __failed: true, id }))
        )
      )
    ).subscribe({
      next: (results) => {
        const failed = results.filter((r: any) => r && typeof r === 'object' && r.__failed);
        if (failed.length) {
          this.toast.show(`Some users could not be deleted (${failed.length})`, 'error');
        } else {
          this.toast.show('Selected users deleted', 'success');
        }
        this.selectedIds = [];
        this.load();
        this.cdr.detectChanges();
      },
      error: (e) => {
        this.toast.showHttpError(e, 'Bulk delete could not be completed. Please try again.');
        this.cdr.detectChanges();
      }
    });
  }

  getRoleBadge(r: string) {
    return { ADMIN: '🛡️', DOCTOR: '🩺', PATIENT: '🧠', CAREGIVER: '🤝' }[r] || '👤';
  }

  getRoleClass(r: string) {
    return { ADMIN: 'role-admin', DOCTOR: 'role-doctor', PATIENT: 'role-patient', CAREGIVER: 'role-care' }[r] || '';
  }

  // ===== Quiz limit =====

  openLimitModal(user: any) {
    this.limitPatient = user;
    this.limitLoading = true;
    this.limitStatus = null;
    this.showLimitModal = true;
    this.cdr.detectChanges();

    this.quizLimitSvc.getStatus(user.id as number).subscribe({
      next: (status) => {
        this.limitStatus = status;
        this.limitValue = status.hasLimit ? status.maxQuizzes : 10;
        this.limitLoading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.limitStatus = {
          patientId: user.id,
          hasLimit: false,
          maxQuizzes: -1,
          completedQuizzes: 0,
          remaining: -1,
          canPlay: true
        };
        this.limitValue = 10;
        this.limitLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  closeLimitModal() {
    this.showLimitModal = false;
    this.limitPatient = null;
    this.limitStatus = null;
  }

  saveLimit() {
    if (!this.limitPatient) return;
    this.limitSaving = true;
    const currentUser = this.auth.getCurrentUser();
    const setByName = currentUser ? `${currentUser.firstName} ${currentUser.lastName}` : 'Admin';

    this.quizLimitSvc.setLimit(
      this.limitPatient.id,
      this.limitValue,
      currentUser?.userId,
      setByName
    ).subscribe({
      next: () => {
        this.toast.show(
          `Quiz limit set to ${this.limitValue} for ${this.limitPatient.firstname} ${this.limitPatient.lastname}`,
          'success'
        );
        this.limitSaving = false;
        this.closeLimitModal();
        this.cdr.detectChanges();
      },
      error: (e) => {
        this.toast.showHttpError(e, 'Could not save the quiz limit. Please try again.');
        this.limitSaving = false;
        this.cdr.detectChanges();
      }
    });
  }

  async removeLimit() {
    if (!this.limitPatient) return;
    const ok = await this.confirm.confirm({
      title: 'Remove quiz limit',
      message: 'Remove the quiz limit for this patient? They will be able to play without a cap.',
      confirmText: 'Remove limit',
      cancelText: 'Cancel',
      danger: true
    });
    if (!ok) return;
    this.limitSaving = true;
    this.quizLimitSvc.removeLimit(this.limitPatient.id).subscribe({
      next: () => {
        this.toast.show(
          `Quiz limit removed for ${this.limitPatient.firstname} ${this.limitPatient.lastname}`,
          'success'
        );
        this.limitSaving = false;
        this.closeLimitModal();
        this.cdr.detectChanges();
      },
      error: (e) => {
        this.toast.showHttpError(e, 'Could not remove the quiz limit. Please try again.');
        this.limitSaving = false;
        this.cdr.detectChanges();
      }
    });
  }
}
