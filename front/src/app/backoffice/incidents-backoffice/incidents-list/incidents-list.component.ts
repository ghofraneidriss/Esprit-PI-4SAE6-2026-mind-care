import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { IncidentService } from '../../../core/services/incident.service';
import { AuthService } from '../../../core/services/auth.service';
import { Incident, IncidentComment, IncidentType } from '../../../core/models/incident.model';

type IncidentStatus = 'OPEN' | 'IN_PROGRESS' | 'RESOLVED';

@Component({
  selector: 'app-incidents-list',
  standalone: false,
  templateUrl: './incidents-list.component.html',
  styleUrls: ['./incidents-list.component.css']
})
export class IncidentsListComponent implements OnInit {
  incidents: Incident[] = [];
  incidentTypes: IncidentType[] = [];
  loading = false;
  error: string | null = null;
  isAddModalOpen = false;
  incidentForm: FormGroup;

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
    private fb: FormBuilder
  ) {
    this.incidentForm = this.fb.group({
      patientId: ['', Validators.required],
      description: ['', Validators.required],
      severityLevel: ['MEDIUM', Validators.required],
      typeId: ['', Validators.required]
    });
  }

  openAddModal() {
    this.incidentForm.reset({
      patientId: '',
      description: '',
      severityLevel: 'MEDIUM',
      typeId: ''
    });
    this.isAddModalOpen = true;
  }

  saveIncident(): void {
    if (this.incidentForm.invalid) return;

    this.loading = true;
    const formVal = this.incidentForm.value;

    // Construct the incident object
    const newIncident: any = {
      patientId: formVal.patientId,
      description: formVal.description,
      severityLevel: formVal.severityLevel,
      status: 'OPEN',
      type: { id: parseInt(formVal.typeId) }
    };

    this.incidentService.createIncident(newIncident).subscribe({
      next: () => {
        this.isAddModalOpen = false;
        this.loadReportedIncidents();
        this.loading = false;
      },
      error: (err) => {
        console.error('Error creating incident:', err);
        this.error = 'Failed to submit report.';
        this.loading = false;
      }
    });
  }

  ngOnInit(): void {
    this.loadReportedIncidents();
    this.loadIncidentTypes();
  }

  loadIncidentTypes(): void {
    this.incidentService.getAllIncidentTypes().subscribe({
      next: (types) => { this.incidentTypes = types; },
      error: (err) => { console.error('Error loading incident types:', err); }
    });
  }

  loadReportedIncidents(): void {
    this.loading = true;
    this.error = null;

    const timer = setTimeout(() => {
      if (this.loading) {
        this.loading = false;
        this.error = "Connection timeout. Please refresh.";
      }
    }, 10000);

    this.incidentService.getReportedIncidents().subscribe({
      next: (incidents) => {
        clearTimeout(timer);
        this.incidents = incidents;
        this.loading = false;
        this.loadPatientNames(incidents);
      },
      error: (err) => {
        clearTimeout(timer);
        this.error = 'Erreur lors du chargement des incidents signalés';
        this.loading = false;
        console.error('Error loading reported incidents:', err);
      }
    });
  }

  loadPatientNames(incidents: Incident[]): void {
    const uniqueIds = [...new Set(incidents.map(i => i.patientId).filter((id): id is number => id != null))];
    uniqueIds.forEach(id => {
      if (!this.patientNames.has(id)) {
        this.authService.getUserById(id).subscribe({
          next: (user) => {
            this.patientNames.set(id, `${user.firstName} ${user.lastName}`);
          },
          error: () => {
            this.patientNames.set(id, `Patient #${id}`);
          }
        });
      }
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

    this.incidentService.getPatientIncidentsHistory(patientId).subscribe({
      next: (data) => {
        this.patientHistory = data;
        this.historyLoading = false;
      },
      error: () => {
        this.historyLoading = false;
      }
    });
  }

  updateIncidentStatus(incidentId: number, newStatus: string): void {
    this.incidentService.updateIncidentStatus(incidentId, newStatus).subscribe({
      next: (updatedIncident) => {
        // Mettre à jour l'incident dans la liste
        const index = this.incidents.findIndex(i => i.id === incidentId);
        if (index !== -1) {
          this.incidents[index] = updatedIncident;
        }
      },
      error: (err) => {
        console.error('Error updating incident status:', err);
      }
    });
  }

  viewDetails(incident: Incident): void {
    this.selectedIncident = incident;
    this.newCommentContent = '';
    this.loadComments(incident.id!);
  }

  loadComments(incidentId: number): void {
    this.commentsLoading = true;
    this.incidentService.getCommentsByIncident(incidentId).subscribe({
      next: (data) => { this.comments = data; this.commentsLoading = false; },
      error: () => { this.commentsLoading = false; }
    });
  }

  addComment(): void {
    if (!this.newCommentContent.trim() || !this.selectedIncident?.id) return;
    this.addingComment = true;
    this.incidentService.addComment(this.selectedIncident.id, {
      content: this.newCommentContent.trim(),
      authorName: 'Admin'
    }).subscribe({
      next: (comment) => {
        this.comments.push(comment);
        this.newCommentContent = '';
        this.addingComment = false;
      },
      error: () => { this.addingComment = false; }
    });
  }

  deleteComment(commentId: number): void {
    this.incidentService.deleteComment(commentId).subscribe({
      next: () => { this.comments = this.comments.filter(c => c.id !== commentId); }
    });
  }

  getSeverityBadgeClass(severity?: string): string {
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

  getStatusBadgeClass(status?: string): string {
    switch (status?.toLowerCase()) {
      case 'résolu':
      case 'resolu':
      case 'resolved':
        return 'bg-success';
      case 'en cours':
      case 'en_cours':
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
