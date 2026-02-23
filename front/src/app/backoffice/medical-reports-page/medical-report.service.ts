import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map, Observable } from 'rxjs';
import { MedicalReport } from './medical-report.model';

@Injectable({ providedIn: 'root' })
export class MedicalReportService {
  private readonly apiUrl = 'http://localhost:8083/api/medical-reports';

  constructor(private readonly http: HttpClient) {}

  getAll(): Observable<MedicalReport[]> {
    return this.http.get<MedicalReport[]>(this.apiUrl);
  }

  getById(id: number): Observable<MedicalReport> {
    return this.http.get<MedicalReport>(`${this.apiUrl}/${id}`);
  }

  create(payload: Partial<MedicalReport>): Observable<MedicalReport> {
    return this.http.post<MedicalReport>(this.apiUrl, payload);
  }

  // Backend uses PUT /api/medical-reports with full object (including reportid)
  update(payload: Partial<MedicalReport>): Observable<MedicalReport> {
    return this.http.put<MedicalReport>(this.apiUrl, payload);
  }

  delete(id: number): Observable<boolean> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`).pipe(map(() => true));
  }
}

