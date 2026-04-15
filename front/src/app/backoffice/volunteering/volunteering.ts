import { Component, OnInit } from '@angular/core';
import { AuthService } from '../../frontoffice/auth/auth.service';
import { VolunteerDirectoryEntry, VolunteerService } from './volunteer.service';

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

    // Directory backed by the volunteers microservice
    volunteers: VolunteerDirectoryEntry[] = [];
    volunteerPresenceMap: Map<number, any> = new Map(); // Track online status by volunteer ID
    selectedMissionForAssignment: Mission | null = null;
    selectedVolunteerForAssignment: VolunteerDirectoryEntry | null = null;
    selectedMissionIdForAssignment: number | null = null;
    isAssignModalOpen = false;
    assignmentLoading = false;
    assignmentError = '';

    constructor(
        public readonly authService: AuthService,
        private readonly volunteerService: VolunteerService
    ) { }

    ngOnInit(): void {
        this.loadMissions();
        this.loadVolunteerDirectory();
        this.loadVolunteerPresence();
        // Reload presence every 30 seconds
        setInterval(() => this.loadVolunteerPresence(), 30000);
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

    loadVolunteerDirectory(): void {
        this.volunteerService.getVolunteerDirectory().subscribe({
            next: (directory) => {
                this.volunteers = directory;
                // Load presence status for each volunteer
                this.loadVolunteerPresence();
            },
            error: (err: unknown) => {
                console.warn('Could not load volunteers directory', err);
            },
        });
    }

    loadVolunteerPresence(): void {
        this.volunteerService.getPresenceStatus().subscribe({
            next: (presenceList) => {
                this.volunteerPresenceMap.clear();
                presenceList.forEach(presence => {
                    this.volunteerPresenceMap.set(presence.userId, presence);
                });
            },
            error: (err: unknown) => {
                console.warn('Could not load volunteer presence', err);
            },
        });
    }

    getVolunteerOnlineStatus(volunteerId: number): string {
        const presence = this.volunteerPresenceMap.get(volunteerId);
        if (!presence) return 'Offline';
        const lastHeartbeat = presence.lastHeartbeat ? new Date(presence.lastHeartbeat) : null;
        const now = new Date();
        const diffSeconds = lastHeartbeat ? (now.getTime() - lastHeartbeat.getTime()) / 1000 : Infinity;
        
        // If last heartbeat was within 90 seconds, consider online
        return diffSeconds < 90 ? 'Online' : 'Offline';
    }

    getVolunteerOnlineStatusClass(volunteerId: number): string {
        return this.getVolunteerOnlineStatus(volunteerId) === 'Online' ? 'status-online' : 'status-offline';
    }

    getVolunteerId(volunteer: VolunteerDirectoryEntry | null | undefined): number | null {
        if (!volunteer) return null;
        return volunteer.userId ?? volunteer.id ?? null;
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

    get filteredVolunteers(): VolunteerDirectoryEntry[] {
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

    openAssignModal(volunteer: VolunteerDirectoryEntry, mission?: Mission | null): void {
        const defaultMission = mission || this.openMissions[0] || null;
        this.selectedVolunteerForAssignment = volunteer;
        this.selectedMissionForAssignment = defaultMission;
        this.selectedMissionIdForAssignment = defaultMission?.id ?? null;
        this.isAssignModalOpen = true;
        this.assignmentError = '';
    }

    closeAssignModal(): void {
        this.isAssignModalOpen = false;
        this.selectedMissionForAssignment = null;
        this.selectedVolunteerForAssignment = null;
        this.selectedMissionIdForAssignment = null;
        this.assignmentError = '';
        this.assignmentLoading = false;
    }

    onAssignmentMissionChange(): void {
        this.selectedMissionForAssignment = this.openMissions.find(
            (mission) => mission.id === this.selectedMissionIdForAssignment
        ) || null;
    }

    confirmAssignment(): void {
        if (!this.selectedVolunteerForAssignment) {
            this.assignmentError = 'No volunteer selected';
            return;
        }
        this.assignMissionToVolunteer(this.selectedVolunteerForAssignment, this.selectedMissionForAssignment || undefined);
    }

    assignMissionToVolunteer(volunteer: VolunteerDirectoryEntry, mission?: Mission): void {
        const targetMission = mission || this.selectedMissionForAssignment;
        const volunteerId = this.getVolunteerId(volunteer);

        if (!targetMission) {
            this.assignmentError = 'No mission selected';
            return;
        }

        if (!volunteerId) {
            this.assignmentError = 'Volunteer ID is missing. Please refresh and try again.';
            return;
        }

        this.assignmentLoading = true;
        this.assignmentError = '';

        // Call the backend with the mission ID and volunteer ID to trigger Twilio
        this.volunteerService.createAssignment(targetMission.id, volunteerId).subscribe({
            next: (assignment) => {
                // Update mission status to reflect assignment
                const missionIdx = this.missions.findIndex(m => m.id === targetMission.id);
                if (missionIdx !== -1) {
                    this.missions[missionIdx].status = 'Assigned';
                    this.missions[missionIdx].assignee = volunteer.name;
                }
                this.closeAssignModal();
                alert(`Mission assigned to ${volunteer.name}! Twilio notification sent.`);
            },
            error: (err) => {
                console.error('Failed to assign mission', err);
                const backendMessage =
                    (typeof err?.error === 'string' ? err.error : null) ||
                    err?.error?.message ||
                    err?.message;
                this.assignmentError = backendMessage || 'Failed to assign mission. Please try again.';
                this.assignmentLoading = false;
            },
        });
    }

    assignVolunteer(volunteer: VolunteerDirectoryEntry): void {
        const open = this.missions.find((m) => m.status === 'Open');
        if (!open) {
            alert('No open missions available to assign.');
            return;
        }
        this.openAssignModal(volunteer, open);
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
