import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
    EntreeCountResponse,
    EntreeSouvenir,
    MediaType,
    ThemeCulturel,
    VoiceRecognitionRequest
} from './souvenirs.model';

@Injectable({
    providedIn: 'root'
})
export class SouvenirsService {
    private souvenirApi = `${this.souvenirApiBase()}/souvenirs`;

    constructor(private http: HttpClient) { }

    createEntree(payload: EntreeSouvenir): Observable<EntreeSouvenir> {
        return this.http.post<EntreeSouvenir>(this.souvenirApi, payload);
    }

    updateEntree(id: number, payload: EntreeSouvenir): Observable<EntreeSouvenir> {
        return this.http.put<EntreeSouvenir>(`${this.souvenirApi}/${id}`, payload);
    }

    deleteEntree(id: number): Observable<void> {
        return this.http.delete<void>(`${this.souvenirApi}/${id}`);
    }

    markTraitee(id: number): Observable<EntreeSouvenir> {
        return this.http.patch<EntreeSouvenir>(`${this.souvenirApi}/${id}/traitee`, {});
    }

    updateVoiceRecognition(id: number, payload: VoiceRecognitionRequest): Observable<EntreeSouvenir> {
        return this.http.patch<EntreeSouvenir>(`${this.souvenirApi}/${id}/voice-recognition`, payload);
    }

    getEntreesByPatient(patientId: number, theme?: ThemeCulturel, mediaType?: MediaType): Observable<EntreeSouvenir[]> {
        let params = new HttpParams();
        if (theme) {
            params = params.set('theme', theme);
        }
        if (mediaType) {
            params = params.set('mediaType', mediaType);
        }
        return this.http.get<EntreeSouvenir[]>(`${this.souvenirApi}/patient/${patientId}`, { params });
    }

    getEntreesByTheme(theme: ThemeCulturel): Observable<EntreeSouvenir[]> {
        return this.http.get<EntreeSouvenir[]>(`${this.souvenirApi}/theme/${theme}`);
    }

    countEntreesByPatient(patientId: number): Observable<EntreeCountResponse> {
        return this.http.get<EntreeCountResponse>(`${this.souvenirApi}/patient/${patientId}/count`);
    }

    private souvenirApiBase(): string {
        if (!environment.production && environment.gatewayBaseUrl) {
            return environment.gatewayBaseUrl.replace(/\/$/, '');
        }
        return environment.apiUrl.replace(/\/$/, '');
    }
}
