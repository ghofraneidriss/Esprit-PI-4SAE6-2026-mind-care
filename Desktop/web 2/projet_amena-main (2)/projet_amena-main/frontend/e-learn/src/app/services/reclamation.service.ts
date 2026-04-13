  import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Reclamation } from '../models/reclamation.model';
import { API_GATEWAY_URL } from './api.config';

export interface ReclamationRatingSummary {
  averageRating: number;
  reclamationCount: number;
  ratedReclamationCount: number;
  rankedCourseCount: number;
}

export interface ReclamationCourseRatingStats {
  courseTitle: string;
  averageRating: number;
  reclamationCount: number;
}

@Injectable({
  providedIn: 'root'
})
export class ReclamationService {
  private readonly baseUrl = `${API_GATEWAY_URL}/reclamations`;

  constructor(private readonly http: HttpClient) {}

  getAll(): Observable<Reclamation[]> {
    return this.http.get<Reclamation[]>(this.baseUrl);
  }

  getById(id: number): Observable<Reclamation> {
    return this.http.get<Reclamation>(`${this.baseUrl}/${id}`);
  }

  create(payload: Reclamation): Observable<Reclamation> {
    return this.http.post<Reclamation>(this.baseUrl, payload);
  }

  update(id: number, payload: Reclamation): Observable<Reclamation> {
    return this.http.put<Reclamation>(`${this.baseUrl}/${id}`, payload);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  getRatingSummary(): Observable<ReclamationRatingSummary> {
    return this.http.get<ReclamationRatingSummary>(`${this.baseUrl}/ratings/summary`);
  }

  getRatingsByCourse(): Observable<ReclamationCourseRatingStats[]> {
    return this.http.get<ReclamationCourseRatingStats[]>(`${this.baseUrl}/ratings/by-course`);
  }

  getRatingsRanking(top = 5): Observable<ReclamationCourseRatingStats[]> {
    return this.http.get<ReclamationCourseRatingStats[]>(`${this.baseUrl}/ratings/ranking?top=${top}`);
  }
}
