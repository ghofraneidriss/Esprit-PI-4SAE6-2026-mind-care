import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { activitiesApiBase } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ReportService {
  private readonly API_URL = `${activitiesApiBase()}/reports`;

  constructor(private http: HttpClient) {}

  downloadPatientPdf(patientId: number): Observable<Blob> {
    return this.http.get(`${this.API_URL}/patient/${patientId}/pdf`, {
      responseType: 'blob',
    });
  }
}
