import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, Subject } from 'rxjs';
import { Incident, IncidentComment, IncidentType } from '../models/incident.model';

@Injectable({ providedIn: 'root' })
export class IncidentService {
  private readonly incidentsApi = 'http://localhost:8087/api/incidents';
  private readonly typesApi = 'http://localhost:8087/api/incident-types';
  readonly refresh$ = new Subject<void>();

  constructor(private readonly http: HttpClient) {}

  getIncidentsByPatient(patientId: number): Observable<Incident[]> {
    return this.http.get<Incident[]>(`${this.incidentsApi}/patient/${patientId}`);
  }

  getIncidentsByCaregiver(caregiverId: number): Observable<Incident[]> {
    return this.http.get<Incident[]>(`${this.incidentsApi}/caregiver/${caregiverId}`);
  }

  getPatientIncidentsHistory(patientId: number): Observable<Incident[]> {
    return this.http.get<Incident[]>(`${this.incidentsApi}/patient/${patientId}/history`);
  }

  getIncidentHistory(): Observable<Incident[]> {
    return this.http.get<Incident[]>(`${this.incidentsApi}/history`);
  }

  getAllActiveIncidents(): Observable<Incident[]> {
    return this.http.get<Incident[]>(`${this.incidentsApi}/active`);
  }

  getReportedIncidents(): Observable<Incident[]> {
    return this.http.get<Incident[]>(`${this.incidentsApi}/reported`);
  }

  getAllIncidentTypes(): Observable<IncidentType[]> {
    return this.http.get<IncidentType[]>(this.typesApi);
  }

  createIncidentType(payload: Partial<IncidentType>): Observable<IncidentType> {
    return this.http.post<IncidentType>(this.typesApi, payload);
  }

  updateIncidentType(typeId: number, payload: Partial<IncidentType>): Observable<IncidentType> {
    return this.http.put<IncidentType>(`${this.typesApi}/${typeId}`, payload);
  }

  deleteIncidentType(typeId: number): Observable<void> {
    return this.http.delete<void>(`${this.typesApi}/${typeId}`);
  }

  createIncident(payload: any): Observable<Incident> {
    return this.http.post<Incident>(this.incidentsApi, payload);
  }

  updateIncident(incidentId: number, payload: Partial<Incident>): Observable<Incident> {
    return this.http.put<Incident>(`${this.incidentsApi}/${incidentId}`, payload);
  }

  deleteIncident(incidentId: number): Observable<void> {
    return this.http.delete<void>(`${this.incidentsApi}/${incidentId}`);
  }

  updateIncidentStatus(incidentId: number, status: string): Observable<Incident> {
    return this.http.patch<Incident>(`${this.incidentsApi}/${incidentId}/status`, { status });
  }

  getCommentsByIncident(incidentId: number): Observable<IncidentComment[]> {
    return this.http.get<IncidentComment[]>(`${this.incidentsApi}/${incidentId}/comments`);
  }

  addComment(incidentId: number, payload: Pick<IncidentComment, 'content' | 'authorName'>): Observable<IncidentComment> {
    return this.http.post<IncidentComment>(`${this.incidentsApi}/${incidentId}/comments`, payload);
  }

  deleteComment(commentId: number): Observable<void> {
    return this.http.delete<void>(`${this.incidentsApi}/comments/${commentId}`);
  }
}
