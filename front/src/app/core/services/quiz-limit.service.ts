import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { activitiesApiBase } from '../../../environments/environment';

export interface QuizLimit {
  id?: number;
  patientId: number;
  maxQuizzes: number;
  setBy?: number;
  setByName?: string;
  updatedAt?: string;
}

export interface QuizLimitStatus {
  patientId: number;
  hasLimit: boolean;
  maxQuizzes: number;
  completedQuizzes: number;
  remaining: number;
  canPlay: boolean;
}

@Injectable({ providedIn: 'root' })
export class QuizLimitService {
  private readonly API_URL = `${activitiesApiBase()}/quiz-limits`;

  constructor(private http: HttpClient) {}

  getAllLimits(): Observable<QuizLimit[]> {
    return this.http.get<QuizLimit[]>(this.API_URL);
  }

  getLimitForPatient(patientId: number): Observable<unknown> {
    return this.http.get(`${this.API_URL}/patient/${patientId}`);
  }

  getStatus(patientId: number): Observable<QuizLimitStatus> {
    return this.http.get<QuizLimitStatus>(`${this.API_URL}/patient/${patientId}/status`);
  }

  setLimit(
    patientId: number,
    maxQuizzes: number,
    setBy?: number,
    setByName?: string
  ): Observable<QuizLimit> {
    return this.http.post<QuizLimit>(this.API_URL, {
      patientId,
      maxQuizzes,
      setBy,
      setByName,
    });
  }

  removeLimit(patientId: number): Observable<unknown> {
    return this.http.delete(`${this.API_URL}/patient/${patientId}`);
  }
}
