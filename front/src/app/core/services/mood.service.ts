import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface MoodEntry {
  id?: number;
  patientId: number;
  caregiverId?: number;
  date: string;       // ISO date string "YYYY-MM-DD"
  score: number;      // 1-5
  note?: string;
  createdAt?: string;
}

@Injectable({ providedIn: 'root' })
export class MoodService {
  private api = '/api/users/mood';

  constructor(private http: HttpClient) {}

  getByPatient(patientId: number): Observable<MoodEntry[]> {
    return this.http.get<MoodEntry[]>(`${this.api}/patient/${patientId}`);
  }

  create(entry: MoodEntry): Observable<MoodEntry> {
    return this.http.post<MoodEntry>(this.api, entry);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/${id}`);
  }
}
