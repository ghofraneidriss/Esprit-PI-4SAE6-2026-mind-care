import { Component, OnInit } from '@angular/core';
import { AuthService } from '../../frontoffice/auth/auth.service';
import { VolunteerService } from './volunteer.service';

export type MissionStatus = 'Open' | 'Assigned' | 'Completed' | 'IN_PROGRESS' | 'OPEN' | 'COMPLETED' | 'CANCELLED';
export type MissionPriority = 'High' | 'Medium' | 'Low';

export interface Mission {
    id: number;
    title: string;
    category: string;
    location: string;
    /** ISO date string from backend, e.g. 2025-01-25T10:00:00 */
    startDate?: string;
    date?: string;         // kept for display compatibility
    duration: string;
    assignee: string;
    priority: MissionPriority;
    status: MissionStatus;
    icon?: string;
    description?: string;
}

export interface Volunteer {
    id: number;
    initials: string;
    name: string;
    city: string;
    verified: boolean;
    skills: string[];
    availability: string;
    rating: number;
    missions: number;
    avatarColor: string;
}

// Map backend status enum → frontend status string
function normalizeStatus(status: string): MissionStatus {
    const map: Record<string, MissionStatus> = {
        OPEN: 'Open',
        IN_PROGRESS: 'Assigned',
        COMPLETED: 'Completed',
        CANCELLED: 'Completed',
    };
    return map[status] ?? (status as MissionStatus);
}

// Map backend priority enum → frontend priority string
function normalizePriority(p: string): MissionPriority {
    if (!p) return 'Medium';
    const up = p.toUpperCase();
    if (up === 'HIGH') return 'High';
    if (up === 'LOW') return 'Low';
    return 'Medium';
}

// Choose a flat icon based on category
function iconForCategory(category: string): string {
    const c = (category || '').toLowerCase();
    if (c.includes('home') || c.includes('visit')) return 'fi fi-rr-home';
    if (c.includes('transport') || c.includes('car')) return 'fi fi-rr-car-side';
    if (c.includes('phone') || c.includes('support')) return 'fi fi-rr-phone-call';
    if (c.includes('shop') || c.includes('errand') || c.includes('grocery')) return 'fi fi-rr-shopping-cart';
    if (c.includes('memory') || c.includes('brain')) return 'fi fi-rr-brain';
    if (c.includes('therapy') || c.includes('group')) return 'fi fi-rr-users';
    if (c.includes('workshop')) return 'fi fi-rr-chalkboard';
    if (c.includes('medical') || c.includes('health')) return 'fi fi-rr-heart-rate';
    return 'fi fi-rr-checkbox';
}

@Component({
    selector: 'app-volunteering-page',
    standalone: false,
    templateUrl: './volunteering.html',
    styleUrls: ['./volunteering.css'],
})
export class VolunteeringPageComponent implements OnInit {
    searchQuery = '';
    volunteerSearch = '';
    showVerifiedOnly = false;
    isCreateModalOpen = false;
    submitAttempted = false;
    isSaving = false;
    formError = '';
    isLoading = false;
    errorMessage = '';

    newMission: Partial<Mission> = {
        title: '',
        category: '',
        location: '',
        date: '',
        duration: '',
        assignee: '',
        priority: 'Medium',
        status: 'Open',
    };

    missions: Mission[] = [];

    // Static volunteer directory (not yet persisted in the backend)
    volunteers: Volunteer[] = [
        {
            id: 1,
            initials: 'MT',
            name: 'Michael Torres',
            city: 'Cambridge, MA',
            verified: true,
            skills: ['Transportation', 'Patient Care', 'Spanish Speaking'],
            availability: 'Weekdays & Weekends',
            rating: 4.9,
            missions: 47,
            avatarColor: '#3b6fd4',
        },
        {
            id: 2,
            initials: 'JL',
            name: 'Jennifer Lee',
            city: 'Boston, MA',
            verified: true,
            skills: ['Phone Support', 'Counseling', 'Elderly Care'],
            availability: 'Weekends only',
            rating: 4.7,
            missions: 31,
            avatarColor: '#0bbcc9',
        },
        {
            id: 3,
            initials: 'RC',
            name: 'Robert Chen',
            city: 'Somerville, MA',
            verified: false,
            skills: ['Home Visit', 'Meal Prep', 'Companionship'],
            availability: 'Evenings',
            rating: 4.5,
            missions: 19,
            avatarColor: '#7c5cbf',
        },
        {
            id: 4,
            initials: 'EP',
            name: 'Emily Parker',
            city: 'Brookline, MA',
            verified: true,
            skills: ['Workshop Help', 'Administrative', 'Social Media'],
            availability: 'Weekdays',
            rating: 4.8,
            missions: 63,
            avatarColor: '#cc7e3a',
        },
    ];

    constructor(
        public readonly authService: AuthService,
        private readonly volunteerService: VolunteerService
    ) { }

    ngOnInit(): void {
        this.loadMissions();
    }

    // ─── Data loading ────────────────────────────────────────────────────────────

    loadMissions(): void {
        this.isLoading = true;
        this.errorMessage = '';
        this.volunteerService.getAll().subscribe({
            next: (data) => {
                this.missions = data.map((m) => ({
                    ...m,
                    status: normalizeStatus(m.status as string),
                    priority: normalizePriority(m.priority as string),
                    icon: iconForCategory(m.category),
                    date: m.startDate
                        ? new Date(m.startDate).toLocaleDateString('en-US', {
                            year: 'numeric',
                            month: 'short',
                            day: 'numeric',
                        })
                        : '—',
                    assignee: m.assignee || 'Unassigned',
                }));
                this.isLoading = false;
            },
            error: (err) => {
                console.error('Failed to load missions', err);
                this.errorMessage = 'Could not load missions from the server.';
                this.isLoading = false;
            },
        });
    }

    // ─── Computed stats ──────────────────────────────────────────────────────────

    get totalMissions(): number {
        return this.missions.length;
    }

    get activeVolunteers(): number {
        return this.volunteers.length;
    }

    get completionRate(): number {
        const completed = this.missions.filter((m) => m.status === 'Completed').length;
        return this.missions.length ? Math.round((completed / this.missions.length) * 100) : 0;
    }

    // ─── Filtered lists ──────────────────────────────────────────────────────────

    get openMissions(): Mission[] {
        return this.missions.filter((m) => m.status === 'Open' && this.matchesSearch(m));
    }

    get assignedMissions(): Mission[] {
        return this.missions.filter((m) => m.status === 'Assigned' && this.matchesSearch(m));
    }

    get completedMissions(): Mission[] {
        return this.missions.filter((m) => m.status === 'Completed' && this.matchesSearch(m));
    }

    get filteredVolunteers(): Volunteer[] {
        const q = this.volunteerSearch.toLowerCase().trim();
        return this.volunteers.filter((v) => {
            const matchesSearch =
                !q ||
                v.name.toLowerCase().includes(q) ||
                v.city.toLowerCase().includes(q) ||
                v.skills.some((s) => s.toLowerCase().includes(q));
            const matchesVerified = !this.showVerifiedOnly || v.verified;
            return matchesSearch && matchesVerified;
        });
    }

    private matchesSearch(m: Mission): boolean {
        const q = this.searchQuery.toLowerCase().trim();
        if (!q) return true;
        return (
            m.title.toLowerCase().includes(q) ||
            (m.category || '').toLowerCase().includes(q) ||
            (m.location || '').toLowerCase().includes(q) ||
            (m.assignee || '').toLowerCase().includes(q)
        );
    }

    // ─── Modal ───────────────────────────────────────────────────────────────────

    openCreateModal(): void {
        this.formError = '';
        this.submitAttempted = false;
        this.isSaving = false;
        this.newMission = {
            title: '',
            category: '',
            location: '',
            date: '',
            duration: '',
            assignee: '',
            priority: 'Medium',
            status: 'Open',
        };
        this.isCreateModalOpen = true;
    }

    closeCreateModal(): void {
        this.isCreateModalOpen = false;
    }

    saveMission(): void {
        this.submitAttempted = true;
        this.formError = '';

        if (!this.newMission.title?.trim()) {
            this.formError = 'Mission title is required.';
            return;
        }
        if (!this.newMission.category?.trim()) {
            this.formError = 'Category is required.';
            return;
        }

        this.isSaving = true;

        const payload = {
            title: this.newMission.title!.trim(),
            category: this.newMission.category!.trim(),
            location: this.newMission.location?.trim() || 'TBD',
            duration: this.newMission.duration?.trim() || '',
            assignee: this.newMission.assignee?.trim() || 'Unassigned',
            priority: (this.newMission.priority || 'Medium').toUpperCase() as any,
            status: 'OPEN' as MissionStatus,
        };

        this.volunteerService.create(payload).subscribe({
            next: (created) => {
                this.missions.push({
                    ...created,
                    status: normalizeStatus(created.status as string),
                    priority: normalizePriority(created.priority as string),
                    icon: iconForCategory(created.category),
                    date: created.startDate
                        ? new Date(created.startDate).toLocaleDateString('en-US', {
                            year: 'numeric',
                            month: 'short',
                            day: 'numeric',
                        })
                        : '—',
                    assignee: created.assignee || 'Unassigned',
                });
                this.isSaving = false;
                this.isCreateModalOpen = false;
            },
            error: (err) => {
                console.error('Failed to create mission', err);
                this.formError = 'Server error: could not save mission.';
                this.isSaving = false;
            },
        });
    }

    // ─── Delete ──────────────────────────────────────────────────────────────────

    deleteMission(mission: Mission): void {
        if (!confirm(`Delete mission "${mission.title}"?`)) return;
        this.volunteerService.delete(mission.id).subscribe({
            next: () => {
                this.missions = this.missions.filter((m) => m.id !== mission.id);
            },
            error: (err) => {
                console.error('Failed to delete mission', err);
                alert('Could not delete mission. Please try again.');
            },
        });
    }

    // ─── Assign ──────────────────────────────────────────────────────────────────

    assignVolunteer(volunteer: Volunteer): void {
        const open = this.missions.find((m) => m.status === 'Open');
        if (!open) {
            alert('No open missions available to assign.');
            return;
        }
        this.volunteerService.assignVolunteer(open.id, volunteer.name).subscribe({
            next: (updated) => {
                const idx = this.missions.findIndex((m) => m.id === updated.id);
                if (idx !== -1) {
                    this.missions[idx] = {
                        ...updated,
                        status: normalizeStatus(updated.status as string),
                        priority: normalizePriority(updated.priority as string),
                        icon: iconForCategory(updated.category),
                        date: updated.startDate
                            ? new Date(updated.startDate).toLocaleDateString('en-US', {
                                year: 'numeric',
                                month: 'short',
                                day: 'numeric',
                            })
                            : '—',
                        assignee: updated.assignee || volunteer.name,
                    };
                }
            },
            error: (err) => {
                console.error('Failed to assign volunteer', err);
                alert('Could not assign volunteer. Please try again.');
            },
        });
    }

    // ─── UI helpers ──────────────────────────────────────────────────────────────

    priorityClass(p: MissionPriority): string {
        if (p === 'High') return 'priority-high';
        if (p === 'Medium') return 'priority-medium';
        return 'priority-low';
    }

    starsArray(rating: number): number[] {
        return Array.from({ length: 5 }, (_, i) => i);
    }

    isFilledStar(index: number, rating: number): boolean {
        return index < Math.floor(rating);
    }
}
