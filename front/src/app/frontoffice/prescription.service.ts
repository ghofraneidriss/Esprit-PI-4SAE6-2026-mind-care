import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

/**
 * Service pour la gestion des prescriptions (Ordonnances) côté Patient.
 */
@Injectable({
  providedIn: 'root'
})
export class PrescriptionService {
  private apiUrl = 'http://localhost:8083/api/patient/prescriptions';

  constructor(private http: HttpClient) { }

  getMyHistory(patientId: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/my-history/${patientId}`);
  }

  getPrescriptionById(id: number): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/${id}`);
  }
}
