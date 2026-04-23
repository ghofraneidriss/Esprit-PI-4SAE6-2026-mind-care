import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

export type ActivityType = 'QUIZ' | 'IMAGE_RECOGNITION' | 'QUESTION_ANSWER';
export type ActivityLevel = 'EASY' | 'MEDIUM' | 'HARD';

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

@Injectable({
    providedIn: 'root'
})
export class ActivityService {
    private apiUrl = 'http://localhost:8084/api/quiz';

    constructor(private http: HttpClient) { }

    getAllActivities(): Observable<Activity[]> {
        return this.http.get<Activity[]>(this.apiUrl).pipe(
            map((activities: Activity[]) => activities.map((activity: any) => ({
                ...activity,
                level: activity.level || activity.difficulty || 'EASY'
            })))
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

    addContent(activityId: number, content: ActivityContent): Observable<ActivityContent> {
        return this.http.post<ActivityContent>(`${this.apiUrl}/${activityId}/content`, content);
    }

    submitResult(result: ActivityResult): Observable<ActivityResult> {
        return this.http.post<ActivityResult>(`${this.apiUrl}/results`, result);
    }

    getResultsByPatient(patientId: number): Observable<ActivityResult[]> {
        return this.http.get<ActivityResult[]>(`${this.apiUrl}/results/patient/${patientId}`);
    }
}
