import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { activitiesApiBase } from '../../../environments/environment';

/** Minimal payload from GET /quiz/:id/preview (fast modal). */
export interface QuizPreviewApi {
  title: string;
  description?: string;
  difficulty?: string;
  questions: {
    text: string;
    optionA: string;
    optionB: string;
    optionC: string;
    optionD?: string;
  }[];
}

export type ActivityType = 'QUIZ' | 'IMAGE_RECOGNITION' | 'QUESTION_ANSWER';
export type ActivityLevel = 'EASY' | 'MEDIUM' | 'HARD';

export interface Quiz {
  id?: number;
  title: string;
  description: string;
  type: ActivityType;
  level: ActivityLevel;
  theme: string;
  difficulty?: string;
  category?: string;
  questions?: Question[];
  status?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface Question {
  id?: number;
  text: string;
  score: number;
  correctAnswer: string;
  optionA: string;
  optionB: string;
  optionC: string;
  optionD?: string;
}

@Injectable({ providedIn: 'root' })
export class QuizService {
  private readonly apiUrl = `${activitiesApiBase()}/quiz`;

  constructor(private http: HttpClient) {}

  private getHeaders(): HttpHeaders {
    return new HttpHeaders({ 'Content-Type': 'application/json' });
  }

  getQuizzes(): Observable<Quiz[]> {
    return this.http.get<Quiz[]>(this.apiUrl).pipe(
      map((quizzes) =>
        quizzes.map(
          (q) =>
            ({
              ...q,
              level: q.level ?? (q.difficulty as ActivityLevel),
            }) as Quiz
        )
      )
    );
  }

  getQuizById(id: number): Observable<Quiz> {
    return this.http.get<Quiz>(`${this.apiUrl}/${id}`).pipe(
      map(
        (q) =>
          ({
            ...q,
            level: q.level ?? (q.difficulty as ActivityLevel),
          }) as Quiz
      )
    );
  }

  /**
   * Lightweight preview for admin modal; falls back to full quiz if /preview is unavailable.
   */
  getQuizPreviewPayload(id: number): Observable<QuizPreviewApi> {
    return this.http.get<QuizPreviewApi>(`${this.apiUrl}/${id}/preview`).pipe(
      catchError(() =>
        this.getQuizById(id).pipe(
          map((q) => ({
            title: q.title,
            description: q.description,
            difficulty: q.difficulty ?? (q.level as string),
            questions: (q.questions || []).map((qn) => ({
              text: qn.text,
              optionA: qn.optionA,
              optionB: qn.optionB,
              optionC: qn.optionC,
              optionD: qn.optionD
            }))
          }))
        )
      )
    );
  }

  createQuiz(quiz: Quiz): Observable<unknown> {
    return this.http.post(this.apiUrl, quiz, { headers: this.getHeaders() });
  }

  updateQuiz(id: number, quiz: Quiz): Observable<unknown> {
    return this.http.put(`${this.apiUrl}/${id}`, quiz, { headers: this.getHeaders() });
  }

  deleteQuiz(id: number): Observable<unknown> {
    return this.http.delete(`${this.apiUrl}/${id}`);
  }

  getQuizzesByTheme(theme: string): Observable<Quiz[]> {
    return this.http.get<Quiz[]>(`${this.apiUrl}/theme/${theme}`);
  }

  getQuizzesByLevel(level: string): Observable<Quiz[]> {
    return this.http.get<Quiz[]>(`${this.apiUrl}/difficulty/${level}`);
  }

  searchQuizzes(title: string): Observable<Quiz[]> {
    return this.http.get<Quiz[]>(`${this.apiUrl}/search?title=${encodeURIComponent(title)}`);
  }
}
