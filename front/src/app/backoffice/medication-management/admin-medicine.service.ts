import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

/**
 * Service pour la gestion administrative des médicaments.
 * Communique avec le microservice ordonnance_et_medicaments (port 8083).
 */
@Injectable({
  providedIn: 'root'
})
export class AdminMedicineService {
  private apiUrl = 'http://localhost:8083/api/admin/medicines';
  private readonly medicamentsApiUrl = 'http://localhost:8083/api/medicaments';

  constructor(private http: HttpClient) { }

  getAll(): Observable<any[]> {
    return this.http.get<any[]>(this.apiUrl);
  }

  getById(id: number): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/${id}`);
  }

  create(medicine: any): Observable<any> {
    return this.http.post<any>(this.apiUrl, medicine);
  }

  update(id: number, medicine: any): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/${id}`, medicine);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  importExcel(file: File): Observable<string> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post(`${this.apiUrl}/import`, formData, { responseType: 'text' });
  }

  suggestNames(query: string): Observable<string[]> {
    return this.http.get<string[]>(`${this.medicamentsApiUrl}/suggest-names?query=${query}`);
  }

  suggestCategories(query: string): Observable<string[]> {
    return this.http.get<string[]>(`${this.medicamentsApiUrl}/suggest-categories?query=${query}`);
  }
}
