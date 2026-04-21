import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface PatientProfile {
    [key: string]: any;
    id?: number;
    userId: number;
    email: string;
    dateOfBirth?: string;
    bloodGroup?: string;
    heightCm?: number;
    weightKg?: number;
    educationLevel?: string;
    caregiverEmergencyNumber?: string;
    isSmoker?: boolean;
    drinksAlcohol?: boolean;
    physicalActivity?: boolean;
    familyHistoryAlzheimer?: boolean;
    hypertension?: boolean;
    type2Diabetes?: boolean;
    hypercholesterolemia?: boolean;
    sleepDisorders?: boolean;
    medications?: string;
    externalCognitiveScore?: number;
}

@Injectable({
    providedIn: 'root'
})
export class PatientProfileService {
    private apiUrl = 'http://localhost:8081/api/profiles'; // Changed to port 8081 matching application.properties

    constructor(private http: HttpClient) { }

    getProfileByUserId(userId: number): Observable<PatientProfile> {
        return this.http.get<PatientProfile>(`${this.apiUrl}/user/${userId}`);
    }

    getProfileByEmail(email: string): Observable<PatientProfile> {
        return this.http.get<PatientProfile>(`${this.apiUrl}/email/${email}`);
    }

    createProfile(profile: PatientProfile): Observable<PatientProfile> {
        return this.http.post<PatientProfile>(this.apiUrl, profile);
    }

    updateProfile(id: number, profile: PatientProfile): Observable<PatientProfile> {
        return this.http.put<PatientProfile>(`${this.apiUrl}/${id}`, profile);
    }

    deleteProfile(id: number): Observable<void> {
        return this.http.delete<void>(`${this.apiUrl}/${id}`);
    }
}
