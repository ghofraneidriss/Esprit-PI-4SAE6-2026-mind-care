import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, throwError } from 'rxjs';
import { catchError, tap, timeout } from 'rxjs/operators';
import { activitiesApiBase } from '../../../environments/environment';

/** Évite un spinner infini si le gateway / activities ne répond pas (Eureka, service arrêté, etc.) */
const PERF_API_TIMEOUT_MS = 20000;

/** Réutilise la dernière réponse pour navigation retour / rechargements rapides (réduit la charge serveur). */
const CACHE_TTL_MS = 45_000;

export interface ThemeScore {
  theme: string;
  label: string;
  icon: string;
  quizCount: number;
  avgScore: number;
  avgWeightedScore: number;
  avgResponseTime: number;
  trend: string;
  level: string;
}

export interface Recommendation {
  type: string;
  priority: string;
  theme: string;
  themeLabel: string;
  currentScore: number;
  message: string;
  suggestedQuizIds: number[];
  suggestedQuizTitles: string[];
}

export interface PatientPerformance {
  patientId: number;
  patientName: string;
  totalQuizzes: number;
  globalScore: number;
  themeScores: ThemeScore[];
  recommendations: Recommendation[];
}

export interface PerfRequestOptions {
  /** Ignorer le cache mémoire (ex. après « Réessayer »). */
  forceRefresh?: boolean;
}

@Injectable({ providedIn: 'root' })
export class PerformanceService {
  private readonly API_URL = `${activitiesApiBase()}/performance`;

  private patientCache = new Map<number, { data: PatientPerformance; at: number }>();
  private allPatientsCache: { data: PatientPerformance[]; at: number } | null = null;

  constructor(private http: HttpClient) {}

  getPatientPerformance(patientId: number, opts?: PerfRequestOptions): Observable<PatientPerformance> {
    if (!opts?.forceRefresh) {
      const hit = this.patientCache.get(patientId);
      if (hit && Date.now() - hit.at < CACHE_TTL_MS) {
        return of(hit.data);
      }
    }
    return this.http.get<PatientPerformance>(`${this.API_URL}/patient/${patientId}`).pipe(
      timeout(PERF_API_TIMEOUT_MS),
      tap((data) => this.patientCache.set(patientId, { data, at: Date.now() })),
      catchError((err) => this.wrapPerfError(err))
    );
  }

  getAllPerformances(opts?: PerfRequestOptions): Observable<PatientPerformance[]> {
    if (!opts?.forceRefresh && this.allPatientsCache && Date.now() - this.allPatientsCache.at < CACHE_TTL_MS) {
      return of(this.allPatientsCache.data);
    }
    return this.http.get<PatientPerformance[]>(`${this.API_URL}/all`).pipe(
      timeout(PERF_API_TIMEOUT_MS),
      tap((data) => (this.allPatientsCache = { data, at: Date.now() })),
      catchError((err) => this.wrapPerfError(err))
    );
  }

  /** Vide les caches (logout, ou avant un rechargement explicite). */
  clearPerformanceCaches(): void {
    this.patientCache.clear();
    this.allPatientsCache = null;
  }

  private wrapPerfError(err: unknown) {
    const name = (err as { name?: string })?.name;
    if (name === 'TimeoutError') {
      return throwError(() => ({ code: 'PERF_TIMEOUT' as const }));
    }
    return throwError(() => err);
  }
}
