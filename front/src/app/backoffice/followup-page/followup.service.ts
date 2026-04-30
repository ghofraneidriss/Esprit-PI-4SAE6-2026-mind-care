import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { FollowUp, FollowUpStats, PatientRisk, CognitiveDeclineResult } from './followup.model';

@Injectable({ providedIn: 'root' })
export class FollowUpService {
  private apiUrl = 'http://localhost:8080/api/followups';
  private statsUrl = 'http://localhost:8080/api/followups/statistics';
  private riskUrl = 'http://localhost:8080/api/followups/patient';

  constructor(private http: HttpClient) {}

  getAll(): Observable<FollowUp[]> {
    return this.http.get<FollowUp[]>(this.apiUrl);
  }

  getById(id: number): Observable<FollowUp> {
    return this.http.get<FollowUp>(`${this.apiUrl}/${id}`);
  }

  create(followUp: Partial<FollowUp>): Observable<FollowUp> {
    return this.http.post<FollowUp>(this.apiUrl, followUp);
  }

  update(id: number, followUp: Partial<FollowUp>): Observable<FollowUp> {
    return this.http.put<FollowUp>(`${this.apiUrl}/${id}`, followUp);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  getStatistics(): Observable<FollowUpStats> {
    return this.http.get<FollowUpStats>(this.statsUrl);
  }

  getPatientRisk(patientId: number): Observable<PatientRisk> {
    return this.http.get<PatientRisk>(`${this.riskUrl}/${patientId}/risk`);
  }

  detectCognitiveDecline(patientId: number): Observable<CognitiveDeclineResult> {
    return this.http.get<CognitiveDeclineResult>(`${this.riskUrl}/${patientId}/cognitive-decline`);
  }
}
