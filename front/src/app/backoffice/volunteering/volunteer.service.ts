import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Mission } from './volunteering';

@Injectable({ providedIn: 'root' })
export class VolunteerService {
    // Direct call to the microservice (or via API gateway at /api/volunteer/missions)
    private readonly baseUrl = 'http://localhost:8085/api/volunteer/missions';

    constructor(private readonly http: HttpClient) { }

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
}
