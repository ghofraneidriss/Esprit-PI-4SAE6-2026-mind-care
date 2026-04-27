import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Appointment {
  id?: number;
  title: string;
  description?: string;
  startDate: string;  // ISO datetime
  endDate?: string;
  patientId?: number;
  caregiverId?: number;
  type: 'VISIT' | 'ACTIVITY' | 'MEDICAL' | 'OTHER';
  color?: string;     // Bootstrap color class: primary, success, warning, danger, info
  createdAt?: string;
}

@Injectable({ providedIn: 'root' })
export class AppointmentService {
  private api = '/api/users/appointments';

  constructor(private http: HttpClient) {}

  getAll(): Observable<Appointment[]> {
    return this.http.get<Appointment[]>(this.api);
  }

  getByPatient(patientId: number): Observable<Appointment[]> {
    return this.http.get<Appointment[]>(`${this.api}/patient/${patientId}`);
  }

  create(appointment: Appointment): Observable<Appointment> {
    return this.http.post<Appointment>(this.api, appointment);
  }

  update(id: number, appointment: Appointment): Observable<Appointment> {
    return this.http.put<Appointment>(`${this.api}/${id}`, appointment);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/${id}`);
  }
}
