import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import type { Mission } from './volunteering';

@Injectable({ providedIn: 'root' })
export class AssignmentService {
    private readonly baseUrl = 'http://localhost:8085/api/assignments';

    constructor(private readonly http: HttpClient) {}

    smartAssign(missionId: number): Observable<Mission> {
        return this.http.post<Mission>(`${this.baseUrl}/${missionId}/smart`, null);
    }

    smartAssignFallback(missionId: number): Observable<Mission> {
        return this.http.post<Mission>(`${this.baseUrl}/${missionId}/smart-fallback`, null);
    }

    fastAssign(missionId: number): Observable<Mission> {
        return this.http.post<Mission>(`${this.baseUrl}/${missionId}/fast`, null);
    }

    assignWithPriority(missionId: number): Observable<Mission> {
        return this.http.post<Mission>(`${this.baseUrl}/${missionId}/priority`, null);
    }

    manualAssign(missionId: number, volunteerId: number): Observable<Mission> {
        return this.http.post<Mission>(`${this.baseUrl}/manual`, { missionId, volunteerId });
    }
}
