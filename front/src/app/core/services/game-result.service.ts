import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { activitiesApiBase } from '../../../environments/environment';

export interface GameResult {
  id?: number;
  patientId: number;
  patientEmail?: string;
  patientName?: string;
  activityType: string;
  activityId: number;
  activityTitle?: string;
  score: number;
  maxScore: number;
  weightedScore?: number;
  difficulty?: string;
  totalQuestions: number;
  correctAnswers: number;
  timeSpentSeconds: number;
  avgResponseTime?: number;
  riskLevel?: string;
  alertSent?: boolean;
  completedAt?: string;
}

export interface RiskAnalysis {
  patientId: number;
  trend: string;
  totalResults: number;
  results: GameResult[];
}

@Injectable({ providedIn: 'root' })
export class GameResultService {
  private readonly API_URL = `${activitiesApiBase()}/game-results`;

  constructor(private http: HttpClient) {}

  createGameResult(result: GameResult): Observable<GameResult> {
    return this.http.post<GameResult>(this.API_URL, result);
  }

  getAllResults(): Observable<GameResult[]> {
    return this.http.get<GameResult[]>(this.API_URL);
  }

  getResultById(id: number): Observable<GameResult> {
    return this.http.get<GameResult>(`${this.API_URL}/${id}`);
  }

  getResultsByPatient(patientId: number): Observable<GameResult[]> {
    return this.http.get<GameResult[]>(`${this.API_URL}/patient/${patientId}`);
  }

  getResultsByActivity(activityType: string, activityId: number): Observable<GameResult[]> {
    return this.http.get<GameResult[]>(`${this.API_URL}/activity/${activityType}/${activityId}`);
  }

  getPatientStats(
    patientId: number,
    activityType: string
  ): Observable<{ totalGames: number; averageScore: number }> {
    return this.http.get<{ totalGames: number; averageScore: number }>(
      `${this.API_URL}/patient/${patientId}/activity/${activityType}/stats`
    );
  }

  getRiskAnalysis(patientId: number): Observable<RiskAnalysis> {
    return this.http.get<RiskAnalysis>(`${this.API_URL}/patient/${patientId}/risk-analysis`);
  }

  updateResult(id: number, result: GameResult): Observable<GameResult> {
    return this.http.put<GameResult>(`${this.API_URL}/${id}`, result);
  }

  deleteResult(id: number): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/${id}`);
  }

  sendAlert(id: number): Observable<{ sent: boolean; message: string }> {
    return this.http.post<{ sent: boolean; message: string }>(`${this.API_URL}/${id}/send-alert`, {});
  }
}
