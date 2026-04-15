import { Component, OnInit } from '@angular/core';
import { AuthService } from '../../frontoffice/auth/auth.service';
import { VolunteerService } from '../volunteering/volunteer.service';

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

interface CompletedMissionCard {
    icon: string;
    title: string;
    category: string;
    patientName: string;
    patientInitial: string;
    patientColor: string;
    date: string;
    rating: number;
}

interface BackendMission {
    id?: number;
    title?: string;
    category?: string;
    assignee?: string;
    duration?: string;
    startDate?: string;
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
    showVerifiedOnly = false;

    // Stats
    totalAssignments = 0;
    activeAssignments = 0;
    completedMissions = 0;
    averageRating = 0;

    assignments: AssignmentHistory[] = [];
    myCompletedMissions: CompletedMissionCard[] = [];

    constructor(
        public readonly authService: AuthService,
        private readonly volunteerService: VolunteerService
    ) { }

    ngOnInit(): void {
        this.loadAssignments();
    }

    loadAssignments(): void {
        this.volunteerService.getAllAssignments().subscribe({
            next: (data: BackendAssignment[]) => {
                this.assignments = (data ?? []).map((a) => this.mapAssignment(a));
                this.myCompletedMissions = this.assignments
                    .filter((a) => a.status === 'Completed')
                    .map((a) => ({
                        icon: a.missionIcon,
                        title: a.missionTitle,
                        category: a.category,
                        patientName: a.patientName,
                        patientInitial: a.patientInitials,
                        patientColor: a.patientColor,
                        date: a.missionDate,
                        rating: a.rating ?? 0,
                    }));

                this.updateStats();
            },
            error: (err) => {
                console.error('Failed to load assignments', err);
                this.assignments = [];
                this.myCompletedMissions = [];
                this.updateStats();
            },
        });
    }

    private mapAssignment(a: BackendAssignment): AssignmentHistory {
        const mission = a.mission ?? {};
        const status = this.normalizeStatus(a.status);
        const volunteerLabel = a.volunteerId ? `Volunteer #${a.volunteerId}` : 'Volunteer';

        return {
            id: a.id ?? 0,
            missionIcon: this.iconForCategory(mission.category),
            missionTitle: mission.title || 'Untitled Mission',
            volunteerName: mission.assignee || volunteerLabel,
            category: mission.category || 'General',
            patientName: 'N/A',
            patientInitials: 'N',
            patientColor: '#3b82f6',
            assignedDate: this.formatDate(a.assignedAt),
            missionDate: this.formatDate(mission.startDate),
            duration: mission.duration || 'N/A',
            rating: a.rating ?? undefined,
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

    private updateStats(): void {
        this.totalAssignments = this.assignments.length;
        this.activeAssignments = this.assignments.filter(a => a.status === 'Assigned' || a.status === 'In Progress').length;
        this.completedMissions = this.assignments.filter(a => a.status === 'Completed').length;

        const ratings = this.assignments
            .map(a => a.rating)
            .filter((rating): rating is number => rating !== undefined && rating > 0);

        this.averageRating = ratings.length
            ? Math.round((ratings.reduce((sum, r) => sum + r, 0) / ratings.length) * 10) / 10
            : 0;
    }

    get filteredAssignments(): AssignmentHistory[] {
        let list = this.assignments;
        if (this.activeFilter !== 'All') {
            list = list.filter(a => a.status === this.activeFilter);
        }
        if (this.searchQuery) {
            const q = this.searchQuery.toLowerCase();
            list = list.filter(a =>
                a.missionTitle.toLowerCase().includes(q) ||
                a.patientName.toLowerCase().includes(q) ||
                a.volunteerName.toLowerCase().includes(q)
            );
        }
        return list;
    }

    setFilter(filter: string) {
        this.activeFilter = filter;
    }

    starsArray(rating: number): number[] {
        return Array.from({ length: 5 }, (_, i) => i);
    }

    isFilledStar(index: number, rating: number): boolean {
        return index < Math.floor(rating);
    }
}
