import { Component, OnInit } from '@angular/core';
import { AuthService } from '../../frontoffice/auth/auth.service';
import { VolunteerService } from '../volunteering/volunteer.service';
import { createPdfBlob, downloadPdfBlob } from '../shared/pdf-utils';

export interface AssignmentHistory {
    id: number;
    missionIcon: string;
    missionTitle: string;
    volunteerName: string;
    category: string;
    patientName: string;
    patientInitials: string;
    patientColor: string;
    assignedDate: string;
    missionDate: string;
    duration: string;
    rating?: number;
    status: 'Assigned' | 'In Progress' | 'Cancelled' | 'Completed';
    statusColor: 'blue' | 'yellow' | 'red' | 'green';
}

interface BackendMission {
    id?: number;
    title?: string;
    category?: string;
    assignee?: string;
    duration?: string;
    startDate?: string;
    description?: string;
}

interface BackendAssignment {
    id?: number;
    volunteerId?: number;
    status?: string;
    rating?: number;
    assignedAt?: string;
    completedAt?: string;
    mission?: BackendMission | null;
}

@Component({
    selector: 'app-assignment-history-page',
    standalone: false,
    templateUrl: './assignment-history.html',
    styleUrls: ['./assignment-history.css'],
})
export class AssignmentHistoryPageComponent implements OnInit {
    searchQuery = '';
    activeFilter = 'All';
    isLoading = false;
    errorMessage = '';
    actionLoadingId: number | null = null;

    totalAssignments = 0;
    activeAssignments = 0;
    completedMissions = 0;
    averageRating = 0;

    assignments: AssignmentHistory[] = [];

    constructor(
        public readonly authService: AuthService,
        private readonly volunteerService: VolunteerService
    ) { }

    get isVolunteer(): boolean {
        return this.authService.isVolunteer();
    }

    get isAdmin(): boolean {
        return this.authService.isAdmin();
    }

    ngOnInit(): void {
        this.loadAssignments();
    }

    loadAssignments(): void {
        const user = this.authService.getLoggedUser();
        this.isLoading = true;
        this.errorMessage = '';

        const source$ = this.isVolunteer && user?.userId
            ? this.volunteerService.getAssignmentsByVolunteer(user.userId)
            : this.volunteerService.getAllAssignments();

        source$.subscribe({
            next: (data: BackendAssignment[]) => {
                this.assignments = (data ?? []).map((a) => this.mapAssignment(a));
                this.updateStats();
                this.isLoading = false;
            },
            error: (err) => {
                console.error('Failed to load assignments', err);
                this.assignments = [];
                this.updateStats();
                this.errorMessage = 'Could not load assignment history.';
                this.isLoading = false;
            },
        });
    }

    get filteredAssignments(): AssignmentHistory[] {
        let list = [...this.assignments];
        if (this.activeFilter !== 'All') {
            list = list.filter((assignment) => assignment.status === this.activeFilter);
        }
        if (this.searchQuery) {
            const q = this.searchQuery.toLowerCase();
            list = list.filter((assignment) =>
                assignment.missionTitle.toLowerCase().includes(q) ||
                assignment.patientName.toLowerCase().includes(q) ||
                assignment.volunteerName.toLowerCase().includes(q) ||
                assignment.category.toLowerCase().includes(q)
            );
        }
        return list;
    }

    setFilter(filter: string): void {
        this.activeFilter = filter;
    }

    downloadAssignmentPdf(assignment: AssignmentHistory): void {
        if (assignment.status !== 'Completed') {
            return;
        }
        const user = this.authService.getLoggedUser();
        const volunteerName = user ? `${user.firstName} ${user.lastName}`.trim() : assignment.volunteerName;
        const safeMissionTitle = this.sanitizeFileName(assignment.missionTitle || 'assignment');
        const title = `Volunteer-Assignment-${safeMissionTitle}-${this.getPdfDateStamp()}`;
        const pages = [[
            'Volunteer Assignment History',
            `Volunteer: ${volunteerName}`,
            `Mission: ${assignment.missionTitle}`,
            `Category: ${assignment.category}`,
            `Status: ${assignment.status}`,
            `Patient: ${assignment.patientName}`,
            `Assigned Date: ${assignment.assignedDate}`,
            `Mission Date: ${assignment.missionDate}`,
            `Duration: ${assignment.duration}`,
            `Rating: ${assignment.rating ?? '-'}`,
            `Generated on: ${new Date().toLocaleString()}`,
        ]];

        downloadPdfBlob(createPdfBlob(title, pages), `${title}.pdf`);
    }

    acceptMission(assignment: AssignmentHistory): void {
        if (assignment.status !== 'Assigned') {
            return;
        }
        this.actionLoadingId = assignment.id;
        this.volunteerService.acceptAssignment(assignment.id).subscribe({
            next: () => {
                this.actionLoadingId = null;
                this.loadAssignments();
            },
            error: (err) => {
                console.error('Failed to accept assignment', err);
                this.errorMessage = 'Could not accept mission. Please try again.';
                this.actionLoadingId = null;
            },
        });
    }

    refuseMission(assignment: AssignmentHistory): void {
        if (assignment.status !== 'Assigned') {
            return;
        }
        this.actionLoadingId = assignment.id;
        this.volunteerService.refuseAssignment(assignment.id).subscribe({
            next: () => {
                this.actionLoadingId = null;
                this.loadAssignments();
            },
            error: (err) => {
                console.error('Failed to refuse assignment', err);
                this.errorMessage = 'Could not refuse mission. Please try again.';
                this.actionLoadingId = null;
            },
        });
    }

    completeMission(assignment: AssignmentHistory): void {
        if (assignment.status !== 'In Progress') {
            return;
        }
        this.actionLoadingId = assignment.id;
        this.volunteerService.completeAssignment(assignment.id).subscribe({
            next: () => {
                this.actionLoadingId = null;
                this.loadAssignments();
            },
            error: (err) => {
                console.error('Failed to complete assignment', err);
                this.errorMessage = 'Could not complete mission. Please try again.';
                this.actionLoadingId = null;
            },
        });
    }

    starsArray(rating: number): number[] {
        return Array.from({ length: 5 }, (_, i) => i);
    }

    isFilledStar(index: number, rating: number): boolean {
        return index < Math.floor(rating);
    }

    private mapAssignment(assignment: BackendAssignment): AssignmentHistory {
        const mission = assignment.mission ?? {};
        const status = this.normalizeStatus(assignment.status);
        const volunteerLabel = this.isVolunteer
            ? this.getVolunteerName()
            : (mission.assignee || (assignment.volunteerId ? `Volunteer #${assignment.volunteerId}` : 'Volunteer'));

        return {
            id: assignment.id ?? 0,
            missionIcon: this.iconForCategory(mission.category),
            missionTitle: mission.title || 'Untitled Mission',
            volunteerName: volunteerLabel,
            category: mission.category || 'General',
            patientName: this.extractPatientName(mission.description),
            patientInitials: this.getInitials(this.extractPatientName(mission.description)),
            patientColor: this.getAccentColor(this.extractPatientName(mission.description)),
            assignedDate: this.formatDate(assignment.assignedAt),
            missionDate: this.formatDate(mission.startDate),
            duration: mission.duration || 'N/A',
            rating: assignment.rating ?? undefined,
            status: status.label,
            statusColor: status.color,
        };
    }

    private normalizeStatus(statusRaw?: string): { label: AssignmentHistory['status']; color: AssignmentHistory['statusColor'] } {
        const status = (statusRaw ?? '').toUpperCase();
        if (status === 'ASSIGNED') return { label: 'Assigned', color: 'blue' };
        if (status === 'IN_PROGRESS') return { label: 'In Progress', color: 'yellow' };
        if (status === 'COMPLETED') return { label: 'Completed', color: 'green' };
        if (status === 'CANCELLED') return { label: 'Cancelled', color: 'red' };
        return { label: 'Assigned', color: 'blue' };
    }

    private iconForCategory(category?: string): string {
        const c = (category || '').toLowerCase();
        if (c.includes('home') || c.includes('visit')) return 'fi fi-rr-home';
        if (c.includes('transport') || c.includes('car')) return 'fi fi-rr-car-side';
        if (c.includes('phone') || c.includes('support')) return 'fi fi-rr-phone-call';
        if (c.includes('shop') || c.includes('errand') || c.includes('grocery')) return 'fi fi-rr-shopping-bag';
        if (c.includes('workshop') || c.includes('group')) return 'fi fi-rr-users';
        return 'fi fi-rr-checkbox';
    }

    private formatDate(value?: string): string {
        if (!value) return 'N/A';
        const date = new Date(value);
        if (Number.isNaN(date.getTime())) return value;
        return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
    }

    private getVolunteerName(): string {
        const user = this.authService.getLoggedUser();
        return user ? `${user.firstName} ${user.lastName}`.trim() : 'Volunteer';
    }

    private extractPatientName(description?: string): string {
        if (!description) {
            return 'N/A';
        }
        const match = description.match(/patient\s*:\s*([^|]+)/i);
        return match?.[1]?.trim() || 'N/A';
    }

    private getInitials(value: string): string {
        const parts = value.split(/\s+/).filter(Boolean);
        if (parts.length === 0) return 'N';
        if (parts.length === 1) return parts[0].charAt(0).toUpperCase();
        return `${parts[0].charAt(0)}${parts[1].charAt(0)}`.toUpperCase();
    }

    private getAccentColor(value: string): string {
        const palette = ['#3b82f6', '#14b8a6', '#8b5cf6', '#f59e0b', '#ef4444', '#10b981'];
        const hash = Array.from(value).reduce((sum, char) => sum + char.charCodeAt(0), 0);
        return palette[hash % palette.length];
    }

    private updateStats(): void {
        this.totalAssignments = this.assignments.length;
        this.activeAssignments = this.assignments.filter((assignment) => assignment.status === 'Assigned' || assignment.status === 'In Progress').length;
        this.completedMissions = this.assignments.filter((assignment) => assignment.status === 'Completed').length;

        const ratings = this.assignments
            .map((assignment) => assignment.rating)
            .filter((rating): rating is number => rating !== undefined && rating > 0);

        this.averageRating = ratings.length
            ? Math.round((ratings.reduce((sum, rating) => sum + rating, 0) / ratings.length) * 10) / 10
            : 0;
    }

    private getPdfDateStamp(): string {
        const now = new Date();
        return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;
    }

    private sanitizeFileName(value: string): string {
        return value
            .toLowerCase()
            .trim()
            .replace(/[^a-z0-9]+/g, '-')
            .replace(/^-+|-+$/g, '') || 'mission';
    }
}
