import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import { IncidentService } from '../../core/services/incident.service';
import { AuthService } from '../../core/services/auth.service';
import { ConfirmDialogService } from '../../core/services/confirm-dialog.service';
import { ToastService } from '../../core/services/toast.service';
import { Incident, IncidentType } from '../../core/models/incident.model';

export type IhSortKey = 'date' | 'type' | 'severity' | 'status';

@Component({
    selector: 'app-patient-incidents',
    standalone: true,
    imports: [CommonModule, RouterModule, FormsModule],
    templateUrl: './patient-incidents.html',
    styleUrls: ['./patient-incidents.css']
})
export class PatientIncidentsComponent implements OnInit {

    incidents: Incident[] = [];
    filteredIncidents: Incident[] = [];
    loading = true;
    serviceUnavailable = false;

    searchQuery = '';
    /** Filtre sévérité — null = tous */
    filterSeverity: 'LOW' | 'MEDIUM' | 'HIGH' | null = null;
    /** Filtre statut — null = tous */
    filterStatus: 'OPEN' | 'IN_PROGRESS' | 'RESOLVED' | null = null;

    sortKey: IhSortKey = 'date';
    sortAsc = false;

    selectedIncident: Incident | null = null;
    isModalOpen = false;

    currentPage = 1;
    /** Lignes par page (data table) */
    readonly pageSize = 10;

    /** Couleurs par option (liste déroulante + état fermé du select) */
    readonly severityOptions: {
        value: 'LOW' | 'MEDIUM' | 'HIGH' | null;
        label: string;
        bg: string;
        fg: string;
    }[] = [
        { value: null, label: 'All severities', bg: '#f8fafc', fg: '#475569' },
        { value: 'LOW', label: 'Low', bg: '#d1fae5', fg: '#065f46' },
        { value: 'MEDIUM', label: 'Medium', bg: '#fef3c7', fg: '#92400e' },
        { value: 'HIGH', label: 'High', bg: '#fecaca', fg: '#991b1b' },
    ];

    readonly statusOptions: {
        value: 'OPEN' | 'IN_PROGRESS' | 'RESOLVED' | null;
        label: string;
        bg: string;
        fg: string;
    }[] = [
        { value: null, label: 'All statuses', bg: '#f8fafc', fg: '#475569' },
        { value: 'OPEN', label: 'Open', bg: '#ede9fe', fg: '#5b21b6' },
        { value: 'IN_PROGRESS', label: 'In progress', bg: '#dbeafe', fg: '#1e40af' },
        { value: 'RESOLVED', label: 'Resolved', bg: '#d1fae5', fg: '#047857' },
    ];

    readonly sortOptions: { key: IhSortKey; label: string; bg: string; fg: string }[] = [
        { key: 'date', label: 'Date', bg: '#ccfbf1', fg: '#0f766e' },
        { key: 'type', label: 'Type', bg: '#e0e7ff', fg: '#3730a3' },
        { key: 'severity', label: 'Severity', bg: '#fef3c7', fg: '#b45309' },
        { key: 'status', label: 'Status', bg: '#ede9fe', fg: '#6d28d9' },
    ];

    constructor(
        private incidentService: IncidentService,
        public authService: AuthService,
        private cdr: ChangeDetectorRef,
        private confirm: ConfirmDialogService,
        private toast: ToastService
    ) { }

    /** Signalement : réservé aux aidants et bénévoles. */
    get canReportIncident(): boolean {
        const r = this.authService.getRole();
        return r === 'CAREGIVER' || r === 'VOLUNTEER';
    }

    /** Patient : vue en cartes (pas le tableau). */
    get isPatientIncidentViewer(): boolean {
        return this.authService.getRole() === 'PATIENT';
    }

    /** Aidant / bénévole : peut éditer et supprimer ses signalements. */
    get canManageIncidents(): boolean {
        return this.canReportIncident;
    }

    incidentTypes: IncidentType[] = [];
    editModalOpen = false;
    editingIncident: Incident | null = null;
    editTypeId: number | null = null;
    editDescription = '';
    editSeverity = 'MEDIUM';
    editStatus = 'OPEN';
    savingEdit = false;

    readonly severityEditOptions = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'] as const;
    readonly statusEditOptions = ['OPEN', 'IN_PROGRESS', 'RESOLVED'] as const;

    /** Libellé court pour l’en-tête type rapport (PDF). */
    incidentRef(inc: Incident): string {
        const n = inc.id;
        return n != null ? `INC-${n}` : 'INC—';
    }

    ngOnInit(): void {
        if (this.canManageIncidents) {
            this.loadIncidentTypes();
        }
        this.loadHistory();
    }

    private loadIncidentTypes(): void {
        this.incidentService.getAllIncidentTypes().subscribe({
            next: (types) => {
                this.incidentTypes = types || [];
            },
            error: () => {
                this.incidentTypes = [];
            }
        });
    }

    trackByIncidentId(index: number, incident: Incident): number {
        return incident.id ?? index;
    }

    loadHistory(): void {
        this.loading = true;
        const userId = this.authService.getUserId();
        const role = this.authService.getRole();

        if (!userId) {
            this.loading = false;
            this.incidents = [];
            this.applyFilters();
            this.cdr.detectChanges();
            return;
        }

        const request$ =
            role === 'CAREGIVER'
                ? this.incidentService.getIncidentsByCaregiver(userId)
                : role === 'VOLUNTEER'
                  ? this.incidentService.getIncidentsByVolunteer(userId)
                  : this.incidentService.getIncidentsByPatient(userId);

        request$.pipe(
            finalize(() => {
                this.loading = false;
                this.cdr.detectChanges();
            })
        ).subscribe({
            next: (data) => {
                this.serviceUnavailable = false;
                this.incidents = data;
                this.applyFilters();
                this.cdr.detectChanges();
            },
            error: () => {
                this.serviceUnavailable = true;
                this.incidents = [];
                this.applyFilters();
                this.cdr.detectChanges();
            }
        });
    }

    applyFilters(): void {
        this.recomputeFiltered();
        this.currentPage = 1;
    }

    clearFilters(): void {
        this.searchQuery = '';
        this.filterSeverity = null;
        this.filterStatus = null;
        this.sortKey = 'date';
        this.sortAsc = false;
        this.applyFilters();
    }

    toggleSortDirection(): void {
        this.sortAsc = !this.sortAsc;
        this.recomputeFiltered();
    }

    severitySelectBoxStyle(): Record<string, string> {
        const o = this.severityOptions.find((x) => x.value === this.filterSeverity);
        if (!o) {
            return {};
        }
        return this.filterSelectBoxStyle(o.bg, o.fg);
    }

    statusSelectBoxStyle(): Record<string, string> {
        const o = this.statusOptions.find((x) => x.value === this.filterStatus);
        if (!o) {
            return {};
        }
        return this.filterSelectBoxStyle(o.bg, o.fg);
    }

    sortSelectBoxStyle(): Record<string, string> {
        const o = this.sortOptions.find((x) => x.key === this.sortKey);
        if (!o) {
            return {};
        }
        return this.filterSelectBoxStyle(o.bg, o.fg);
    }

    private filterSelectBoxStyle(bg: string, fg: string): Record<string, string> {
        return {
            background: bg,
            color: fg,
            borderColor: this.mixBorderColor(fg),
        };
    }

    /** Bordure légèrement teintée pour rester lisible sur fonds colorés */
    private mixBorderColor(fg: string): string {
        return `${fg}33`;
    }

    private recomputeFiltered(): void {
        const q = this.searchQuery.toLowerCase().trim();
        const list = this.incidents.filter((i) => {
            const matchSearch =
                !q ||
                (i.type?.name || '').toLowerCase().includes(q) ||
                (i.description || '').toLowerCase().includes(q);
            const matchSev = this.filterSeverity == null || i.severityLevel === this.filterSeverity;
            const matchStat = this.filterStatus == null || i.status === this.filterStatus;
            return matchSearch && matchSev && matchStat;
        });
        this.filteredIncidents = this.sortList(list);
    }

    private sortList(list: Incident[]): Incident[] {
        const dir = this.sortAsc ? 1 : -1;
        const severityOrder: Record<string, number> = { LOW: 1, MEDIUM: 2, HIGH: 3, CRITICAL: 4 };
        return [...list].sort((a, b) => {
            switch (this.sortKey) {
                case 'date': {
                    const ta = new Date(a.incidentDate || 0).getTime();
                    const tb = new Date(b.incidentDate || 0).getTime();
                    return (ta - tb) * dir;
                }
                case 'type':
                    return (a.type?.name || '').localeCompare(b.type?.name || '', undefined, { sensitivity: 'base' }) * dir;
                case 'severity': {
                    const va = severityOrder[a.severityLevel || ''] ?? 0;
                    const vb = severityOrder[b.severityLevel || ''] ?? 0;
                    return (va - vb) * dir;
                }
                case 'status':
                    return (a.status || '').localeCompare(b.status || '') * dir;
                default:
                    return 0;
            }
        });
    }

    get paginatedIncidents(): Incident[] {
        const start = (this.currentPage - 1) * this.pageSize;
        return this.filteredIncidents.slice(start, start + this.pageSize);
    }

    get totalPages(): number {
        return Math.ceil(this.filteredIncidents.length / this.pageSize) || 1;
    }

    /** Plage affichée pour le pied de tableau (ex. 1–10 sur 24) */
    get rangeStart(): number {
        if (this.filteredIncidents.length === 0) {
            return 0;
        }
        return (this.currentPage - 1) * this.pageSize + 1;
    }

    get rangeEnd(): number {
        return Math.min(this.currentPage * this.pageSize, this.filteredIncidents.length);
    }

    /** Indices pour lignes vides (compléter jusqu’à `pageSize` lignes par page) */
    get tablePlaceholderIndices(): number[] {
        const pad = Math.max(0, this.pageSize - this.paginatedIncidents.length);
        return Array.from({ length: pad }, (_, i) => i);
    }

    trackByPadIndex(index: number): number {
        return index;
    }

    openDetailModal(incident: Incident): void {
        this.selectedIncident = incident;
        this.isModalOpen = true;
    }

    closeModal(): void {
        this.isModalOpen = false;
        this.selectedIncident = null;
    }

    getSeverityColor(severity: string | undefined): string {
        switch (severity) {
            case 'CRITICAL': return '#B91C1C';
            case 'HIGH': return '#EF4444';
            case 'MEDIUM': return '#F59E0B';
            case 'LOW': return '#10B981';
            default: return '#6B7280';
        }
    }

    getStatusColor(status: string | undefined): string {
        switch (status) {
            case 'OPEN': return '#8B5CF6';
            case 'IN_PROGRESS': return '#3B82F6';
            case 'RESOLVED': return '#10B981';
            default: return '#6B7280';
        }
    }

    /** Bordure gauche de ligne — teinte selon sévérité */
    rowAccentClass(incident: Incident): string {
        switch (incident.severityLevel) {
            case 'HIGH': return 'ih-tr--sev-high';
            case 'MEDIUM': return 'ih-tr--sev-med';
            case 'LOW': return 'ih-tr--sev-low';
            default: return 'ih-tr--sev-na';
        }
    }

    openEditModal(incident: Incident, ev: Event): void {
        ev.stopPropagation();
        ev.preventDefault();
        this.editingIncident = incident;
        this.editTypeId = incident.type?.id ?? null;
        this.editDescription = incident.description || '';
        this.editSeverity = String(incident.severityLevel || 'MEDIUM');
        this.editStatus = String(incident.status || 'OPEN');
        this.editModalOpen = true;
    }

    closeEditModal(): void {
        this.editModalOpen = false;
        this.editingIncident = null;
        this.savingEdit = false;
    }

    openEditFromDetail(): void {
        if (!this.selectedIncident || !this.canManageIncidents) return;
        const inc = { ...this.selectedIncident };
        this.editTypeId = inc.type?.id ?? null;
        this.editDescription = inc.description || '';
        this.editSeverity = String(inc.severityLevel || 'MEDIUM');
        this.editStatus = String(inc.status || 'OPEN');
        this.editingIncident = inc;
        this.closeModal();
        this.editModalOpen = true;
    }

    saveEdit(): void {
        if (!this.editingIncident?.id || this.editTypeId == null) {
            this.toast.show('Please choose a type.', 'error');
            return;
        }
        this.savingEdit = true;
        const payload: Incident = {
            ...this.editingIncident,
            description: this.editDescription,
            severityLevel: this.editSeverity as Incident['severityLevel'],
            status: this.editStatus as Incident['status'],
            type: { id: this.editTypeId },
        };
        this.incidentService.updateIncident(this.editingIncident.id, payload).subscribe({
            next: () => {
                this.savingEdit = false;
                this.toast.show('Incident updated.', 'success');
                this.closeEditModal();
                this.loadHistory();
            },
            error: () => {
                this.toast.show('Could not save changes.', 'error');
                this.savingEdit = false;
                this.cdr.detectChanges();
            }
        });
    }

    async confirmDeleteIncident(incident: Incident, ev: Event): Promise<void> {
        ev.stopPropagation();
        ev.preventDefault();
        const ok = await this.confirm.confirm({
            title: 'Delete this incident?',
            message: 'This action cannot be undone.',
            confirmText: 'Delete',
            danger: true,
        });
        if (!ok || incident.id == null) return;
        const id = incident.id;
        this.incidentService.deleteIncident(id).subscribe({
            next: () => {
                this.toast.show('Incident deleted.', 'success');
                if (this.selectedIncident?.id === incident.id) {
                    this.closeModal();
                }
                this.loadHistory();
            },
            error: () => this.toast.show('Could not delete.', 'error'),
        });
    }

    async deleteFromDetail(): Promise<void> {
        const id = this.selectedIncident?.id;
        if (id == null) return;
        const ok = await this.confirm.confirm({
            title: 'Delete this incident?',
            message: 'This action cannot be undone.',
            confirmText: 'Delete',
            danger: true,
        });
        if (!ok) return;
        this.incidentService.deleteIncident(id).subscribe({
            next: () => {
                this.toast.show('Incident deleted.', 'success');
                this.closeModal();
                this.loadHistory();
            },
            error: () => this.toast.show('Could not delete.', 'error'),
        });
    }
}
