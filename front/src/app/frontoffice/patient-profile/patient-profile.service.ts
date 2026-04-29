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
    amedicaments?: string[];
    allergies?: string[];
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

    /**
     * Vérifie si un patient a une allergie au médicament sélectionné.
     * Appelle le endpoint JPQL: GET /api/profiles/{userId}/check-allergy
     * @returns Liste des allergies correspondantes (vide = pas d'allergie)
     */
    checkAllergyForMedicine(userId: number, medicineName: string, therapeuticFamily: string): Observable<string[]> {
        const params = `medicineName=${encodeURIComponent(medicineName)}&therapeuticFamily=${encodeURIComponent(therapeuticFamily)}`;
        return this.http.get<string[]>(`${this.apiUrl}/${userId}/check-allergy?${params}`);
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

    // Autocomplete suggestions
    private medicamentApiUrl = 'http://localhost:8083/api/medicaments';

    suggestAllergyCategories(query: string): Observable<string[]> {
        console.log(`[PatientProfileService] Fetching allergy suggestions for: ${query}`);
        return this.http.get<string[]>(`${this.medicamentApiUrl}/suggest-categories?query=${query}`);
    }

    suggestMedicationNames(query: string): Observable<string[]> {
        console.log(`[PatientProfileService] Fetching medication suggestions for: ${query}`);
        return this.http.get<string[]>(`${this.medicamentApiUrl}/suggest-names?query=${query}`);
    }
}
