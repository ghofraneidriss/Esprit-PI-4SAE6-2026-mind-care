import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
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
    patientName?: string;
    patientId?: number;
}

interface PatientOption {
    userId: number;
    firstName: string;
    lastName: string;
    email?: string;
    role?: string;
}

interface LocationSuggestion {
    displayName: string;
    lat: string;
    lon: string;
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
    patients: PatientOption[] = [];
    patientsLoading = false;
    patientsError = '';
    locationSuggestions: LocationSuggestion[] = [];
    locationLoading = false;
    locationSearchTimeout: ReturnType<typeof setTimeout> | null = null;
    selectedLocationSuggestion: LocationSuggestion | null = null;

    newMission: Partial<Mission> = {
        title: '',
        category: '',
        location: '',
        date: '',
        duration: '',
        assignee: '',
        patientId: undefined,
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
        private readonly http: HttpClient,
        private readonly volunteerService: VolunteerService
    ) { }

    get isAdmin(): boolean {
        return this.authService.isAdmin();
    }

    ngOnInit(): void {
        this.loadMissions();
        this.loadVolunteerDirectory();
        this.loadPatients();
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
                    patientName: this.extractPatientName(m.description),
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

    loadPatients(): void {
        this.patientsLoading = true;
        this.patientsError = '';
        this.http.get<PatientOption[]>('http://localhost:8082/api/users').subscribe({
            next: (users) => {
                this.patients = (users ?? [])
                    .filter((user) => (user.role || '').toUpperCase() === 'PATIENT')
                    .sort((a, b) => `${a.firstName} ${a.lastName}`.localeCompare(`${b.firstName} ${b.lastName}`));
                this.patientsLoading = false;
            },
            error: (err) => {
                console.warn('Could not load patients list', err);
                this.patients = [];
                this.patientsError = 'Could not load patients from users service.';
                this.patientsLoading = false;
            },
        });
    }

    onLocationInput(value: string): void {
        this.newMission.location = value;
        this.selectedLocationSuggestion = null;
        this.locationSuggestions = [];

        if (this.locationSearchTimeout) {
            clearTimeout(this.locationSearchTimeout);
            this.locationSearchTimeout = null;
        }

        const query = value.trim();
        if (query.length < 3) {
            return;
        }

        this.locationSearchTimeout = setTimeout(() => {
            this.searchLocations(query);
        }, 300);
    }

    searchLocations(query: string): void {
        this.locationLoading = true;
        this.http.get<any[]>(
            `https://nominatim.openstreetmap.org/search?format=jsonv2&addressdetails=1&limit=6&q=${encodeURIComponent(query)}`
        ).subscribe({
            next: (results) => {
                this.locationSuggestions = (results ?? []).map((result) => ({
                    displayName: result.display_name,
                    lat: result.lat,
                    lon: result.lon,
                }));
                this.locationLoading = false;
            },
            error: (err) => {
                console.warn('Could not load location suggestions', err);
                this.locationSuggestions = [];
                this.locationLoading = false;
            },
        });
    }

    chooseLocationSuggestion(suggestion: LocationSuggestion): void {
        this.newMission.location = suggestion.displayName;
        this.selectedLocationSuggestion = suggestion;
        this.locationSuggestions = [];
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
            (m.assignee || '').toLowerCase().includes(q) ||
            (m.patientName || '').toLowerCase().includes(q)
        );
    }

    // ─── Modal ───────────────────────────────────────────────────────────────────

    openCreateModal(): void {
        if (!this.isAdmin) {
            this.formError = 'Only admins can create missions.';
            return;
        }
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
            patientId: undefined,
            priority: 'Medium',
            status: 'Open',
        };
        this.locationSuggestions = [];
        this.selectedLocationSuggestion = null;
        this.isCreateModalOpen = true;
    }

    closeCreateModal(): void {
        this.isCreateModalOpen = false;
        this.locationSuggestions = [];
        this.selectedLocationSuggestion = null;
        if (this.locationSearchTimeout) {
            clearTimeout(this.locationSearchTimeout);
            this.locationSearchTimeout = null;
        }
    }

    saveMission(): void {
        if (!this.isAdmin) {
            this.formError = 'Only admins can create missions.';
            return;
        }
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
        if (!this.newMission.patientId) {
            this.formError = 'Patient is required.';
            return;
        }

        this.isSaving = true;
        const selectedPatient = this.patients.find((patient) => patient.userId === this.newMission.patientId);
        const patientName = selectedPatient ? `${selectedPatient.firstName} ${selectedPatient.lastName}`.trim() : '';

        const payload = {
            title: this.newMission.title!.trim(),
            category: this.newMission.category!.trim(),
            location: this.newMission.location?.trim() || 'TBD',
            duration: this.newMission.duration?.trim() || '',
            assignee: this.newMission.assignee?.trim() || 'Unassigned',
            description: this.buildMissionDescription(patientName),
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
                    patientName: patientName || this.extractPatientName(created.description),
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
        if (!this.isAdmin) {
            alert('Only admins can delete missions.');
            return;
        }
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
        if (!this.isAdmin) {
            return;
        }
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
        if (!this.isAdmin) {
            this.assignmentError = 'Only admins can assign missions.';
            return;
        }
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
        if (!this.isAdmin) {
            return;
        }
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

    getPatientLabel(patient: PatientOption): string {
        return `${patient.firstName} ${patient.lastName}`.trim();
    }

    private extractPatientName(description?: string): string {
        if (!description) {
            return 'Unassigned';
        }

        const match = description.match(/patient\s*:\s*([^|]+)/i);
        return match?.[1]?.trim() || 'Unassigned';
    }

    private buildMissionDescription(patientName: string): string {
        if (!patientName) {
            return '';
        }
        return `Patient: ${patientName}`;
    }

    trackByLocationSuggestion(_index: number, suggestion: LocationSuggestion): string {
        return suggestion.displayName;
    }
}
