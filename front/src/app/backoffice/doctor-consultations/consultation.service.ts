import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Consultation } from './consultation.model';

@Injectable({
    providedIn: 'root'
})
export class ConsultationService {
    private apiUrl = 'http://localhost:8081/api/consultations'; // Assuming 8081 for traitement_et_consultation

    constructor(private http: HttpClient) { }

    getAll(): Observable<Consultation[]> {
        return this.http.get<Consultation[]>(this.apiUrl);
    }

    /**
     * COMMENTAIRE POUR LE REPERAGE (Demande utilisateur) :
     * Interroge le backend Spring Boot en lui passant les critères de filtre actuels.
     * La recherche et le filtre sont gérés coté serveur.
     */
    getFiltered(stage?: string, searchTerm?: string): Observable<Consultation[]> {
        let params = new URLSearchParams();
        if (stage && stage !== 'all') params.set('stage', stage);
        if (searchTerm && searchTerm.trim() !== '') params.set('searchTerm', searchTerm.trim());

        return this.http.get<Consultation[]>(`${this.apiUrl}/filter?${params.toString()}`);
    }

    getById(id: number): Observable<Consultation> {
        return this.http.get<Consultation>(`${this.apiUrl}/${id}`);
    }

    create(consultation: Consultation): Observable<Consultation> {
        return this.http.post<Consultation>(this.apiUrl, consultation);
    }

    update(id: number, consultation: Consultation): Observable<Consultation> {
        return this.http.put<Consultation>(`${this.apiUrl}/${id}`, consultation);
    }

    delete(id: number): Observable<void> {
        return this.http.delete<void>(`${this.apiUrl}/${id}`);
    }
}
