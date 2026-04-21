import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { activitiesApiBase } from '../../../environments/environment';
import type { ActivityType, ActivityLevel } from './quiz.service';

export interface Activity {
  id?: number;
  title: string;
  description: string;
  type: ActivityType;
  level: ActivityLevel;
  category?: string;
  contents?: ActivityContent[];
}

export interface ActivityContent {
  id?: number;
  question: string;
  optionA?: string;
  optionB?: string;
  optionC?: string;
  correctAnswer?: string;
  imageUrl?: string;
}

export interface ActivityResult {
  id?: number;
  patientId: number;
  score: number;
  duration: number;
  date?: string;
  activityId: number;
}

@Injectable({ providedIn: 'root' })
export class ActivityService {
  private readonly apiUrl = `${activitiesApiBase()}/quiz`;

  constructor(private http: HttpClient) {}

  getAllActivities(): Observable<Activity[]> {
    return this.http.get<Activity[]>(this.apiUrl).pipe(
      map((activities: Activity[]) =>
        activities.map((activity: Activity & { difficulty?: string }) => ({
          ...activity,
          level: activity.level || (activity.difficulty as ActivityLevel) || 'EASY',
        }))
      )
    );
  }

  getActivityById(id: number): Observable<Activity> {
    return this.http.get<Activity>(`${this.apiUrl}/${id}`);
  }

  createActivity(activity: Activity): Observable<Activity> {
    return this.http.post<Activity>(this.apiUrl, activity);
  }

  updateActivity(id: number, activity: Activity): Observable<Activity> {
    return this.http.put<Activity>(`${this.apiUrl}/${id}`, activity);
  }

  deleteActivity(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
