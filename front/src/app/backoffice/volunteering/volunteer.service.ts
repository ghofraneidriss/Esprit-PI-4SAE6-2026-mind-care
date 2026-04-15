import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { catchError, Observable, throwError } from 'rxjs';
import { Client } from '@stomp/stompjs';
import { AuthService } from '../../frontoffice/auth/auth.service';
import { Mission } from './volunteering';

export interface VolunteerDirectoryEntry {
    userId?: number;
    id?: number; // backward compatibility if older payloads still use id
    initials: string;
    name: string;
    email?: string;
    verified: boolean;
    city: string;
    availability: string;
    skills: string[];
    rating: number;
    missions: number;
    avatarColor: string;
    onlineStatus?: string;
    lastSeen?: string;
}

export interface VolunteerPresenceDTO {
    userId: number;
    displayName: string;
    status: string;
    lastHeartbeat: string;
    connectedAt: string;
    disconnectedAt: string;
    totalOnlineSeconds: number;
}

@Injectable({ providedIn: 'root' })
export class VolunteerService {
    // Direct call to the microservice (or via API gateway)
    private readonly baseUrl = 'http://localhost:8085/api/volunteer/missions';
    private readonly presenceUrl = 'http://localhost:8085/api/volunteer/presence';
    private readonly volunteersBase = 'http://localhost:8085/api/volunteers';
    private readonly assignmentUrl = 'http://localhost:8085/api/volunteer/assignments';

    private stompClient: Client | null = null;
    private heartbeatInterval: ReturnType<typeof setInterval> | null = null;
    private sessionHeartbeatTimer: ReturnType<typeof setInterval> | null = null;
    private currentUserId: number | null = null;
    private currentSessionId: string | null = null;
    private readonly realtimePresenceEnabled = false;

    constructor(
        private readonly http: HttpClient,
        private readonly authService: AuthService
    ) { }

    connectWebSocket(): void {
        if (!this.realtimePresenceEnabled) {
            return;
        }

        if (this.stompClient && this.stompClient.active) {
            return;
        }

        const user = this.authService.getLoggedUser();
        if (!user) {
            return;
        }

        this.currentUserId = user.userId;
        this.currentSessionId = this.currentSessionId || this.buildSessionId(user.userId);

        void import('sockjs-client')
            .then((module) => {
                const SockJsCtor = (module.default || module) as unknown as { new(url: string): WebSocket };

                this.stompClient = new Client({
                    webSocketFactory: () => new SockJsCtor('http://localhost:8085/ws'),
                    connectHeaders: {
                        userId: String(user.userId),
                        displayName: `${user.firstName} ${user.lastName}`,
                    },
                    reconnectDelay: 5000,
                    heartbeatIncoming: 4000,
                    heartbeatOutgoing: 4000,
                    debug: () => { },
                });

                this.stompClient.onConnect = () => {
                    this.stompClient?.publish({
                        destination: '/app/presence.connect',
                        body: JSON.stringify({
                            userId: this.currentUserId,
                            displayName: `${user.firstName} ${user.lastName}`,
                            sessionId: this.currentSessionId,
                        }),
                    });
                    this.heartbeatInterval = setInterval(() => {
                        if (!this.currentUserId) return;
                        this.stompClient?.publish({
                            destination: '/app/presence.heartbeat',
                            body: JSON.stringify({ userId: this.currentUserId }),
                        });
                    }, 20000);
                    this.startSessionHeartbeat();
                };

                this.stompClient.onDisconnect = () => {
                    if (this.heartbeatInterval) {
                        clearInterval(this.heartbeatInterval);
                        this.heartbeatInterval = null;
                    }
                    this.stopSessionHeartbeat();
                };

                this.stompClient.activate();
            })
            .catch((error) => {
                console.error('Failed to initialize volunteer WebSocket client', error);
                this.stompClient = null;
            });
    }

    disconnectWebSocket(): void {
        if (!this.realtimePresenceEnabled) {
            return;
        }

        if (this.stompClient && this.stompClient.active) {
            if (this.currentUserId) {
                this.stompClient.publish({
                    destination: '/app/presence.disconnect',
                    body: JSON.stringify({ userId: this.currentUserId }),
                });
            }
            this.stompClient.deactivate();
        }

        this.currentUserId = null;
        this.currentSessionId = null;
        this.stopSessionHeartbeat();
    }

    markOnline(userId: number, displayName?: string): Observable<void> {
        const payload = {
            userId,
            displayName: displayName?.trim() || null,
            sessionId: this.currentSessionId || this.buildSessionId(userId),
            userAgent: typeof navigator === 'undefined' ? null : navigator.userAgent,
        };
        this.currentSessionId = payload.sessionId;
        return this.http.post<void>(`${this.volunteersBase}/${userId}/login`, payload);
    }

    markOffline(userId: number): Observable<void> {
        return this.http.post<void>(`${this.volunteersBase}/${userId}/logout`, null);
    }

    startSessionHeartbeat(): void {
        this.stopSessionHeartbeat();
        if (!this.currentUserId || typeof window === 'undefined') {
            return;
        }
        this.sessionHeartbeatTimer = window.setInterval(() => {
            this.http.post<void>(`${this.volunteersBase}/heartbeat/${this.currentUserId}`, null)
                .subscribe({ error: () => { } });
        }, 30000);
    }

    stopSessionHeartbeat(): void {
        if (this.sessionHeartbeatTimer) {
            clearInterval(this.sessionHeartbeatTimer);
            this.sessionHeartbeatTimer = null;
        }
    }

    getVolunteerDirectory(): Observable<VolunteerDirectoryEntry[]> {
        return this.http.get<VolunteerDirectoryEntry[]>(`${this.volunteersBase}/directory`);
    }

    getVolunteerById(volunteerId: number): Observable<VolunteerDirectoryEntry> {
        return this.http.get<VolunteerDirectoryEntry>(`${this.volunteersBase}/${volunteerId}`);
    }

    /** Fetch all missions */
    getAll(): Observable<Mission[]> {
        return this.http.get<Mission[]>(this.baseUrl);
    }

    /** Create a new mission */
    create(mission: Partial<Mission>): Observable<Mission> {
        return this.http.post<Mission>(this.baseUrl, mission);
    }

    /** Delete a mission by id */
    delete(id: number): Observable<void> {
        return this.http.delete<void>(`${this.baseUrl}/${id}`);
    }

    /** Assign a volunteer to the first open mission */
    assignVolunteer(missionId: number, volunteerName: string): Observable<Mission> {
        return this.http.post<Mission>(`${this.baseUrl}/${missionId}/assign`, {
            volunteerName,
        });
    }

    /** Update a mission (e.g. change status) */
    update(id: number, mission: Partial<Mission>): Observable<Mission> {
        return this.http.put<Mission>(`${this.baseUrl}/${id}`, mission);
    }

    /** Presence helpers */
    getAllPresences(): Observable<VolunteerPresenceDTO[]> {
        return this.http.get<VolunteerPresenceDTO[]>(this.presenceUrl);
    }

    getActivePresences(): Observable<VolunteerPresenceDTO[]> {
        return this.http.get<VolunteerPresenceDTO[]>(`${this.presenceUrl}/active`);
    }

    getPresenceByUserId(userId: number): Observable<VolunteerPresenceDTO> {
        return this.http.get<VolunteerPresenceDTO>(`${this.presenceUrl}/${userId}`);
    }

    getActiveCount(): Observable<{ activeCount: number }> {
        return this.http.get<{ activeCount: number }>(`${this.presenceUrl}/active/count`);
    }

    getStatusSummary(): Observable<Record<string, number>> {
        return this.http.get<Record<string, number>>(`${this.presenceUrl}/summary`);
    }

    // Assignment Methods
    getAllAssignments(): Observable<any[]> {
        return this.http.get<any[]>(this.assignmentUrl);
    }

    getAssignmentsByVolunteer(volunteerId: number): Observable<any[]> {
        return this.http.get<any[]>(`${this.assignmentUrl}/volunteer/${volunteerId}`);
    }

    updateAssignment(id: number, assignment: any): Observable<any> {
        return this.http.put<any>(`${this.assignmentUrl}/${id}`, assignment);
    }

    // Create manual assignment - triggers Twilio notification
    createAssignment(missionId: number, volunteerId: number, notes?: string): Observable<any> {
        const legacyAssignmentBody = {
            mission: { id: missionId },
            volunteerId,
            volunteerUserId: volunteerId,
            notes: notes || '',
        };

        const payload = {
            missionId,
            volunteerId,
            notes: notes || '',
        };

        // Use /api/volunteers route because it is publicly accessible in current backend security config.
        // Backend reuses AssignmentService.manualAssign(...) and triggers Twilio notification.
        return this.http.post<any>(`${this.volunteersBase}/assign-and-notify`, payload).pipe(
            catchError((err) => {
                // Only fallback when the new route does not exist on backend.
                if (err?.status === 404) {
                    return this.http.post<any>(`${this.assignmentUrl}`, legacyAssignmentBody);
                }
                return throwError(() => err);
            })
        );
    }

    // Get presence status for all volunteers
    getPresenceStatus(): Observable<VolunteerPresenceDTO[]> {
        return this.http.get<VolunteerPresenceDTO[]>(`${this.presenceUrl}/all`);
    }

    // Get presence status for a specific volunteer
    getVolunteerPresence(userId: number): Observable<VolunteerPresenceDTO> {
        return this.http.get<VolunteerPresenceDTO>(`${this.presenceUrl}/${userId}`);
    }

    // Get all online volunteers
    getOnlineVolunteers(): Observable<VolunteerPresenceDTO[]> {
        return this.http.get<VolunteerPresenceDTO[]>(`${this.presenceUrl}/online`);
    }

    private buildSessionId(userId: number): string {
        return `volunteer-${userId}-${Date.now()}`;
    }
}
