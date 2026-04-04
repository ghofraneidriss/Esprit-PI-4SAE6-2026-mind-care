import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

export interface UserSummary {
  userId: number;
  firstName: string;
  lastName: string;
  email: string;
  role: string;
}

@Injectable({ providedIn: 'root' })
export class UserApiService {
  private readonly apiUrl = 'http://localhost:8082/api/users';

  constructor(private readonly http: HttpClient) {}

  getAllUsers(): Observable<UserSummary[]> {
    return this.http.get<UserSummary[]>(this.apiUrl);
  }

  getPatients(): Observable<UserSummary[]> {
    return this.getAllUsers().pipe(
      map(users => users.filter(u => u.role === 'PATIENT'))
    );
  }

  getCaregivers(): Observable<UserSummary[]> {
    return this.getAllUsers().pipe(
      map(users => users.filter(u => u.role === 'CAREGIVER'))
    );
  }
}
