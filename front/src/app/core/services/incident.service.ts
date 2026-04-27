import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { asyncScheduler } from 'rxjs';
import { Observable, Subject, finalize, of, tap, timeout, shareReplay } from 'rxjs';
import { observeOn } from 'rxjs/operators';
import { Incident, IncidentComment, IncidentType, PatientStats } from '../models/incident.model';
import { environment } from '../../../environments/environment';

@Injectable({
    providedIn: 'root'
})
export class IncidentService {

    /** Incidents : direct 8083 en dev (rapide) ou `/api` via gateway si useIncidentDirect false */
    private readonly incidentBase = environment.useIncidentDirect
        ? environment.incidentApiUrl
        : environment.apiUrl;

    private static readonly TIMEOUT_MS = 8000;

    private _refresh$ = new Subject<void>();

    /** Une seule requête HTTP pour les types tant que la session n’a pas invalidé le cache */
    private incidentTypesCache: IncidentType[] | null = null;
    private incidentTypesInFlight: Observable<IncidentType[]> | null = null;

    constructor(private http: HttpClient) { }

    get refresh$() {
        return this._refresh$;
    }

    private clearIncidentTypesClientCache(): void {
        this.incidentTypesCache = null;
        this.incidentTypesInFlight = null;
    }

    private t8<T>(obs: Observable<T>): Observable<T> {
        return obs.pipe(timeout(IncidentService.TIMEOUT_MS));
    }

    // --- INCIDENTS ---

    getAllActiveIncidents(): Observable<Incident[]> {
        return this.t8(this.http.get<Incident[]>(`${this.incidentBase}/incidents`));
    }

    getIncidentHistory(): Observable<Incident[]> {
        return this.t8(this.http.get<Incident[]>(`${this.incidentBase}/incidents/history`));
    }

    getPatientIncidentsHistory(patientId: number): Observable<Incident[]> {
        return this.t8(this.http.get<Incident[]>(`${this.incidentBase}/incidents/patient/${patientId}/history`));
    }

    getIncidentsByPatient(patientId: number): Observable<Incident[]> {
        return this.t8(this.http.get<Incident[]>(`${this.incidentBase}/incidents/patient/${patientId}`));
    }

    getIncidentsByCaregiver(caregiverId: number): Observable<Incident[]> {
        return this.t8(this.http.get<Incident[]>(`${this.incidentBase}/incidents/caregiver/${caregiverId}`));
    }

    getIncidentsByVolunteer(volunteerId: number): Observable<Incident[]> {
        return this.t8(this.http.get<Incident[]>(`${this.incidentBase}/incidents/volunteer/${volunteerId}`));
    }

    getIncidentById(id: number): Observable<Incident> {
        return this.t8(this.http.get<Incident>(`${this.incidentBase}/incidents/${id}`));
    }

    createIncident(incident: Incident): Observable<Incident> {
        return this.http.post<Incident>(`${this.incidentBase}/incidents`, incident)
            .pipe(tap(() => this._refresh$.next()));
    }

    updateIncident(id: number, incident: Incident): Observable<Incident> {
        return this.http.put<Incident>(`${this.incidentBase}/incidents/${id}`, incident)
            .pipe(tap(() => this._refresh$.next()));
    }

    updateIncidentStatus(id: number, status: string): Observable<Incident> {
        return this.http.patch<Incident>(`${this.incidentBase}/incidents/${id}/status`, { status })
            .pipe(tap(() => this._refresh$.next()));
    }

    deleteIncident(id: number): Observable<void> {
        return this.http.delete<void>(`${this.incidentBase}/incidents/${id}`)
            .pipe(tap(() => this._refresh$.next()));
    }

    /**
     * Back-office : CAREGIVER par défaut (admin). Médecin : source=DOCTOR et reporterId = userId courant.
     */
    getReportedIncidents(source: string = 'CAREGIVER', reporterId?: number | null): Observable<Incident[]> {
        let params = new HttpParams().set('source', source);
        if (reporterId != null && reporterId > 0) {
            params = params.set('reporterId', String(reporterId));
        }
        return this.t8(
            this.http.get<Incident[]>(`${this.incidentBase}/incidents/reported`, { params })
        );
    }

    // --- INCIDENT TYPES ---

    /**
     * Liste des types : mise en cache côté client (instantané après le 1er chargement).
     * Invalider via création / mise à jour / suppression de type.
     */
    getAllIncidentTypes(): Observable<IncidentType[]> {
        if (this.incidentTypesCache !== null) {
            // Émission différée : évite une mise à jour synchrone pendant le 1er cycle de détection Angular.
            return of(this.incidentTypesCache).pipe(observeOn(asyncScheduler));
        }
        if (this.incidentTypesInFlight) {
            return this.incidentTypesInFlight;
        }
        this.incidentTypesInFlight = this.t8(
            this.http.get<IncidentType[]>(`${this.incidentBase}/incident-types`)
        ).pipe(
            tap((types) => { this.incidentTypesCache = types; }),
            finalize(() => { this.incidentTypesInFlight = null; }),
            shareReplay(1)
        );
        return this.incidentTypesInFlight;
    }

    createIncidentType(type: IncidentType): Observable<IncidentType> {
        return this.http.post<IncidentType>(`${this.incidentBase}/incident-types`, type)
            .pipe(
                tap(() => {
                    this.clearIncidentTypesClientCache();
                    this._refresh$.next();
                })
            );
    }

    updateIncidentType(id: number, type: IncidentType): Observable<IncidentType> {
        return this.http.put<IncidentType>(`${this.incidentBase}/incident-types/${id}`, type)
            .pipe(
                tap(() => {
                    this.clearIncidentTypesClientCache();
                    this._refresh$.next();
                })
            );
    }

    deleteIncidentType(id: number): Observable<void> {
        return this.http.delete<void>(`${this.incidentBase}/incident-types/${id}`)
            .pipe(
                tap(() => {
                    this.clearIncidentTypesClientCache();
                    this._refresh$.next();
                })
            );
    }

    // --- COMMENTS ---

    getCommentsByIncident(incidentId: number): Observable<IncidentComment[]> {
        return this.t8(this.http.get<IncidentComment[]>(`${this.incidentBase}/incidents/${incidentId}/comments`));
    }

    addComment(incidentId: number, comment: { content: string; authorId?: number; authorName?: string }): Observable<IncidentComment> {
        return this.http.post<IncidentComment>(`${this.incidentBase}/incidents/${incidentId}/comments`, comment);
    }

    deleteComment(commentId: number): Observable<void> {
        return this.http.delete<void>(`${this.incidentBase}/incidents/comments/${commentId}`);
    }

    // --- PATIENT STATS ---

    getPatientStats(): Observable<PatientStats[]> {
        return this.t8(this.http.get<PatientStats[]>(`${this.incidentBase}/incidents/patient-stats`));
    }

    getPatientStatsById(patientId: number): Observable<PatientStats> {
        return this.t8(this.http.get<PatientStats>(`${this.incidentBase}/incidents/patient-stats/${patientId}`));
    }

    sendPatientStatsByEmail(patientId: number, email: string): Observable<any> {
        return this.http.post(`${this.incidentBase}/incidents/patient-stats/${patientId}/send-email`, { email });
    }
}
