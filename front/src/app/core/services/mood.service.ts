import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface MoodEntry {
  id?: number;
  patientId: number;
  date: string;
  score: number;
  note?: string;
}

@Injectable({ providedIn: 'root' })
export class MoodService {
  private readonly moodApi = 'http://localhost:8087/api/moods';

  constructor(private readonly http: HttpClient) {}

  getByPatient(patientId: number): Observable<MoodEntry[]> {
    return this.http.get<MoodEntry[]>(`${this.moodApi}/patient/${patientId}`);
  }

  create(payload: MoodEntry): Observable<MoodEntry> {
    return this.http.post<MoodEntry>(this.moodApi, payload);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.moodApi}/${id}`);
  }
}
