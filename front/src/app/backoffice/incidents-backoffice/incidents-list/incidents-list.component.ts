import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  OnInit,
} from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { forkJoin, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { IncidentService } from '../../../core/services/incident.service';
import { AuthService } from '../../../core/services/auth.service';
import { Incident, IncidentComment, PatientStats } from '../../../core/models/incident.model';
import { PatientRegistrationOption } from '../../../core/models/user.model';

@Component({
  selector: 'app-incidents-list',
  standalone: false,
  templateUrl: './incidents-list.component.html',
  styleUrls: ['./incidents-list.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class IncidentsListComponent implements OnInit {
  incidents: Incident[] = [];
  /** First type from API (required by backend); no category picker in UI. */
  defaultIncidentTypeId: number | null = null;
  /** True jusqu’au 1er résultat de `loadReportedIncidents` (évite flash vide + OnPush figé). */
  loading = true;
  error: string | null = null;
  isAddModalOpen = false;
  /** Incident analytics (same charts as former /admin/incidents-analytics). */
  isAnalyticsModalOpen = false;
  incidentForm: FormGroup;

  /** Comptes patient (liste déroulante) — tous les rôles PATIENT. */
  patients: PatientRegistrationOption[] = [];

  isDoctor = false;

  /** Calcul avancé de gravité (API patient-stats). */
  gravityPatientId: number | null = null;
  gravityStats: PatientStats | null = null;
  gravityLoading = false;

  // Patient names map: patientId → full name
  patientNames: Map<number, string> = new Map();

  // Patient history modal
  isHistoryModalOpen = false;
  historyPatientName = '';
  historyPatientId: number | null = null;
  patientHistory: Incident[] = [];
  historyLoading = false;

  // Comments
  selectedIncident: Incident | null = null;
  comments: IncidentComment[] = [];
  newCommentContent = '';
  commentsLoading = false;
  addingComment = false;

  constructor(
    private incidentService: IncidentService,
    private authService: AuthService,
    private fb: FormBuilder,
    private cdr: ChangeDetectorRef
  ) {
    this.incidentForm = this.fb.group({
      patientId: ['', Validators.required],
      description: ['', Validators.required],
      severityLevel: ['MEDIUM', Validators.required]
    });
  }

  openAnalyticsModal(): void {
    this.isAnalyticsModalOpen = true;
    this.cdr.markForCheck();
  }

  closeAnalyticsModal(): void {
    this.isAnalyticsModalOpen = false;
    this.cdr.markForCheck();
  }

  openAddModal() {
    this.incidentForm.reset({
      patientId: '',
      description: '',
      severityLevel: 'MEDIUM'
    });
    this.isAddModalOpen = true;
    this.cdr.markForCheck();
  }

  saveIncident(): void {
    if (this.incidentForm.invalid || this.defaultIncidentTypeId == null) return;

    this.loading = true;
    this.cdr.markForCheck();
    const formVal = this.incidentForm.value;

    const uid = this.authService.getUserId();
    const role = this.authService.getRole();
    this.incidentService
      .createIncident({
        patientId: Number(formVal.patientId),
        description: formVal.description,
        severityLevel: formVal.severityLevel,
        status: 'OPEN',
        type: { id: this.defaultIncidentTypeId },
        source: role === 'DOCTOR' ? 'DOCTOR' : 'ADMIN',
        reporterUserId: uid ?? undefined
      } as Incident)
      .subscribe({
      next: () => {
        this.isAddModalOpen = false;
        this.loadReportedIncidents();
      },
      error: (err) => {
        console.error('Error creating incident:', err);
        this.error = 'Failed to submit report.';
        this.loading = false;
        this.cdr.markForCheck();
      }
    });
  }

  ngOnInit(): void {
    this.isDoctor = this.authService.getRole() === 'DOCTOR';
    this.cdr.markForCheck();
    /** Évite NG0100 / vue figée : ne pas lancer 3 flux dans le même tour que le 1er CD. */
    queueMicrotask(() => {
      this.loadPatients();
      this.loadReportedIncidents();
      this.loadDefaultIncidentType();
    });
  }

  loadPatients(): void {
    this.authService.getPatientsForRegistration().subscribe({
      next: (list) => {
        this.patients = list ?? [];
        this.cdr.markForCheck();
      },
      error: () => {
        this.patients = [];
        this.cdr.markForCheck();
      }
    });
  }

  computeGravityScore(): void {
    if (this.gravityPatientId == null || this.gravityPatientId <= 0) {
      return;
    }
    this.gravityLoading = true;
    this.gravityStats = null;
    this.cdr.markForCheck();
    this.incidentService.getPatientStatsById(this.gravityPatientId).subscribe({
      next: (s) => {
        this.gravityStats = s;
        this.gravityLoading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.gravityLoading = false;
        this.cdr.markForCheck();
      }
    });
  }

  getRiskBadgeClass(risk: string): string {
    switch (risk?.toUpperCase()) {
      case 'CRITICAL':
        return 'bg-danger';
      case 'HIGH':
        return 'bg-warning text-dark';
      case 'MODERATE':
        return 'bg-info text-dark';
      default:
        return 'bg-success';
    }
  }

  /** Backend requires a type id; we use the first type from the service (no category UI). */
  loadDefaultIncidentType(): void {
    this.incidentService.getAllIncidentTypes().subscribe({
      next: (types) => {
        const sorted = [...(types ?? [])].sort((a, b) => (a.id ?? 0) - (b.id ?? 0));
        this.defaultIncidentTypeId = sorted[0]?.id ?? null;
        this.cdr.markForCheck();
      },
      error: () => {
        this.defaultIncidentTypeId = null;
        this.cdr.markForCheck();
      }
    });
  }

  loadReportedIncidents(): void {
    this.loading = true;
    this.error = null;
    this.cdr.markForCheck();

    const timer = setTimeout(() => {
      if (this.loading) {
        this.loading = false;
        this.error = "Connection timeout. Please refresh.";
        this.cdr.markForCheck();
      }
    }, 10000);

    const uid = this.authService.getUserId();
    const source = this.isDoctor ? 'DOCTOR' : 'CAREGIVER';
    const reporterId = this.isDoctor ? uid : null;

    this.incidentService.getReportedIncidents(source, reporterId).subscribe({
      next: (incidents) => {
        clearTimeout(timer);
        const list = incidents ?? [];
        this.incidents = list;
        this.loading = false;
        this.loadPatientNames(list);
        this.cdr.markForCheck();
      },
      error: (err) => {
        clearTimeout(timer);
        this.error = 'Failed to load reported incidents';
        this.loading = false;
        console.error('Error loading reported incidents:', err);
        this.cdr.markForCheck();
      }
    });
  }

  loadPatientNames(incidents: Incident[]): void {
    const uniqueIds = [...new Set(incidents.map((i) => i.patientId).filter((id): id is number => id != null))];
    const missing = uniqueIds.filter((id) => !this.patientNames.has(id));
    if (missing.length === 0) {
      return;
    }
    forkJoin(
      missing.map((id) =>
        this.authService.getUserById(id).pipe(
          map((user) => ({ id, name: `${user.firstName} ${user.lastName}`.trim() })),
          catchError(() => of({ id, name: `Patient #${id}` }))
        )
      )
    ).subscribe((rows) => {
      const next = new Map(this.patientNames);
      for (const r of rows) {
        next.set(r.id, r.name);
      }
      this.patientNames = next;
      this.cdr.markForCheck();
    });
  }

  getPatientName(patientId: number | null | undefined): string {
    if (!patientId) return '—';
    return this.patientNames.get(patientId) || `Patient #${patientId}`;
  }

  viewPatientHistory(patientId: number | null | undefined): void {
    if (!patientId) return;
    this.historyPatientId = patientId;
    this.historyPatientName = this.patientNames.get(patientId) || `Patient #${patientId}`;
    this.patientHistory = [];
    this.historyLoading = true;
    this.isHistoryModalOpen = true;
    this.cdr.markForCheck();

    this.incidentService.getPatientIncidentsHistory(patientId).subscribe({
      next: (data) => {
        this.patientHistory = data;
        this.historyLoading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.historyLoading = false;
        this.cdr.markForCheck();
      }
    });
  }

  updateIncidentStatus(incidentId: number, newStatus: string): void {
    this.incidentService.updateIncidentStatus(incidentId, newStatus).subscribe({
      next: (updatedIncident) => {
        const index = this.incidents.findIndex((i) => i.id === incidentId);
        if (index !== -1) {
          const next = [...this.incidents];
          next[index] = updatedIncident;
          this.incidents = next;
        }
        this.cdr.markForCheck();
      },
      error: (err) => {
        console.error('Error updating incident status:', err);
      }
    });
  }

  viewDetails(incident: Incident): void {
    this.selectedIncident = incident;
    this.newCommentContent = '';
    this.cdr.markForCheck();
    this.loadComments(incident.id!);
  }

  loadComments(incidentId: number): void {
    this.commentsLoading = true;
    this.incidentService.getCommentsByIncident(incidentId).subscribe({
      next: (data) => {
        this.comments = data;
        this.commentsLoading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.commentsLoading = false;
        this.cdr.markForCheck();
      }
    });
  }

  addComment(): void {
    if (!this.newCommentContent.trim() || !this.selectedIncident?.id) return;
    this.addingComment = true;
    this.cdr.markForCheck();
    this.incidentService.addComment(this.selectedIncident.id, {
      content: this.newCommentContent.trim(),
      authorName: this.authService.getFullName() || 'Staff'
    }).subscribe({
      next: (comment) => {
        this.comments.push(comment);
        this.newCommentContent = '';
        this.addingComment = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.addingComment = false;
        this.cdr.markForCheck();
      }
    });
  }

  deleteComment(commentId: number): void {
    this.incidentService.deleteComment(commentId).subscribe({
      next: () => {
        this.comments = this.comments.filter((c) => c.id !== commentId);
        this.cdr.markForCheck();
      }
    });
  }

  getSeverityBadgeClass(severity: string): string {
    switch (severity?.toUpperCase()) {
      case 'LOW':
        return 'bg-success';
      case 'MEDIUM':
        return 'bg-warning';
      case 'HIGH':
        return 'bg-danger';
      case 'CRITICAL':
        return 'bg-danger text-white';
      default:
        return 'bg-secondary';
    }
  }

  getStatusBadgeClass(status: string): string {
    switch (status?.toLowerCase()) {
      case 'résolu':
      case 'resolu':
      case 'resolved':
        return 'bg-success';
      case 'en cours':
      case 'en_cours':
      case 'in progress':
      case 'in_progress':
        return 'bg-warning';
      case 'nouveau':
      case 'open':
        return 'bg-info';
      case 'critique':
        return 'bg-danger';
      default:
        return 'bg-secondary';
    }
  }
}
