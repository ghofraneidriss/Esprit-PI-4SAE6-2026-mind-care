import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface IncidentStats {
  totalActive: number;
  totalHistory: number;
  bySeverity: { [key: string]: number };
  byStatus: { [key: string]: number };
  byMonth: { [key: string]: number };
}

@Injectable({ providedIn: 'root' })
export class AnalyticsService {
  private api = '/api';

  constructor(private http: HttpClient) {}

  getIncidentStats(): Observable<IncidentStats> {
    return this.http.get<IncidentStats>(`${this.api}/incidents/stats`);
  }
}
