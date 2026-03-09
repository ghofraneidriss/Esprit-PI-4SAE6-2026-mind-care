import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Recommendation, RecommendationStatus, RecommendationType } from './recommendation.model';

@Injectable({
    providedIn: 'root'
})
export class RecommendationService {
    // Port 8085 = recommendation_service (Direct Access)
    private apiUrl = 'http://localhost:8085/api/recommendations';
    private eventUrl = 'http://localhost:8085/api/events';

    constructor(private http: HttpClient) { }


    // --- Recommendation Methods ---
    getAll(): Observable<Recommendation[]> {
        return this.http.get<Recommendation[]>(this.apiUrl);
    }

    getById(id: number): Observable<Recommendation> {
        return this.http.get<Recommendation>(`${this.apiUrl}/${id}`);
    }

    create(recommendation: Recommendation): Observable<Recommendation> {
        return this.http.post<Recommendation>(this.apiUrl, recommendation);
    }

    update(id: number, recommendation: Recommendation): Observable<Recommendation> {
        return this.http.put<Recommendation>(`${this.apiUrl}/${id}`, recommendation);
    }

    delete(id: number): Observable<void> {
        return this.http.delete<void>(`${this.apiUrl}/${id}`);
    }

    approve(id: number): Observable<Recommendation> {
        return this.http.patch<Recommendation>(`${this.apiUrl}/${id}/approve`, {});
    }

    // --- MedicalEvent Methods (Medical Games) ---
    getAllEvents(): Observable<any[]> {
        return this.http.get<any[]>(this.eventUrl);
    }

    createEvent(event: any): Observable<any> {
        return this.http.post<any>(this.eventUrl, event);
    }

    deleteEvent(id: number): Observable<void> {
        return this.http.delete<void>(`${this.eventUrl}/${id}`);
    }

    searchRecommendations(query: string): Observable<Recommendation[]> {
        return this.http.get<Recommendation[]>(`${this.apiUrl}/search?query=${query}`);
    }

    searchEvents(query: string): Observable<any[]> {
        return this.http.get<any[]>(`${this.eventUrl}/search?query=${query}`);
    }
}

