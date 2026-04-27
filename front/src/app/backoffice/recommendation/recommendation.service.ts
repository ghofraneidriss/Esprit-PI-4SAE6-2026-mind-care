import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { timeout } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import {
    AutoRecommendationGenerateRequest,
    ClinicalEscalationAlert,
    JoinMedicalEventRequest,
    MedicalEvent,
    MedicalEventParticipation,
    MedicalEventType,
    ParticipantRanking,
    ParticipantType,
    Puzzle,
    PuzzleCreateRequest,
    PuzzleLeaderboardEntry,
    PuzzleSession,
    PuzzleSessionStartResponse,
    PuzzleSessionSubmitRequest,
    Recommendation,
    RecommendationStats,
    RecommendationStatus,
    ScoreResponse,
    StreakResponse,
    SudokuGame,
    SudokuSessionStartResponse,
    SudokuSessionSubmitRequest,
    SudokuSessionResponse,
} from './recommendation.model';

@Injectable({
    providedIn: 'root'
})
export class RecommendationService {
    private apiUrl = `${this.recommendationApiBase()}/recommendations`;
    private eventUrl = `${this.recommendationApiBase()}/events`;
    private puzzleUrl = `${this.recommendationApiBase()}/puzzles`;
    private sudokuUrl = `${this.recommendationApiBase()}/sudoku`;

    constructor(private http: HttpClient) { }

    getAll(): Observable<Recommendation[]> {
        return this.http.get<Recommendation[]>(this.apiUrl);
    }

    getById(id: number): Observable<Recommendation> {
        return this.http.get<Recommendation>(`${this.apiUrl}/${id}`);
    }

    create(recommendation: Recommendation): Observable<Recommendation> {
        return this.http.post<Recommendation>(this.apiUrl, {
            content: recommendation.content,
            type: recommendation.type,
            doctorId: recommendation.doctorId,
            patientId: recommendation.patientId,
            priority: recommendation.priority ?? 0,
            expirationDate: recommendation.expirationDate ?? null,
            generatedMedicalEventId: recommendation.generatedMedicalEventId ?? null
        }).pipe(timeout(15000));
    }

    update(id: number, recommendation: Recommendation | RecommendationStatus): Observable<Recommendation> {
        if (typeof recommendation === 'string') {
            return this.http.put<Recommendation>(`${this.apiUrl}/${id}`, { status: recommendation }).pipe(timeout(15000));
        }

        return this.http.put<Recommendation>(`${this.apiUrl}/${id}/details`, {
            content: recommendation.content,
            type: recommendation.type,
            doctorId: recommendation.doctorId,
            patientId: recommendation.patientId,
            priority: recommendation.priority ?? 0,
            expirationDate: recommendation.expirationDate ?? null,
            generatedMedicalEventId: recommendation.generatedMedicalEventId ?? null
        }).pipe(timeout(15000));
    }

    delete(id: number): Observable<void> {
        return this.http.delete<void>(`${this.apiUrl}/${id}`);
    }

    approve(id: number): Observable<Recommendation> {
        return this.http.patch<Recommendation>(`${this.apiUrl}/${id}/approve`, {});
    }

    accept(id: number): Observable<Recommendation> {
        return this.http.post<Recommendation>(`${this.apiUrl}/${id}/accept`, {});
    }

    dismiss(id: number): Observable<Recommendation> {
        return this.http.post<Recommendation>(`${this.apiUrl}/${id}/dismiss`, {});
    }

    getByPatient(patientId: number): Observable<Recommendation[]> {
        return this.http.get<Recommendation[]>(`${this.apiUrl}/patient/${patientId}`);
    }

    getActiveByPatient(patientId: number): Observable<Recommendation[]> {
        return this.http.get<Recommendation[]>(`${this.apiUrl}/patient/${patientId}/active`);
    }

    getSortedByPatient(patientId: number): Observable<Recommendation[]> {
        return this.http.get<Recommendation[]>(`${this.apiUrl}/patient/${patientId}/sorted`);
    }

    getRecommendationStats(patientId: number): Observable<RecommendationStats> {
        return this.http.get<RecommendationStats>(`${this.apiUrl}/patient/${patientId}/stats`);
    }

    archiveExpiredRecommendations(): Observable<number> {
        return this.http.post<number>(`${this.apiUrl}/archive-expired`, {});
    }

    getAllEvents(): Observable<MedicalEvent[]> {
        return this.http.get<MedicalEvent[]>(this.eventUrl);
    }

    getEventById(id: number): Observable<MedicalEvent> {
        return this.http.get<MedicalEvent>(`${this.eventUrl}/${id}`);
    }

    createEvent(event: MedicalEvent): Observable<MedicalEvent> {
        const now = new Date();
        const end = new Date(now);
        end.setDate(now.getDate() + 7);

        return this.http.post<MedicalEvent>(this.eventUrl, {
            title: event.title,
            description: event.description ?? '',
            type: event.type ?? MedicalEventType.OTHER,
            difficulty: event.difficulty,
            patientId: event.patientId ?? 1,
            familyId: event.familyId ?? null,
            startDate: this.serializeMedicalEventDate(event.startDate, now),
            endDate: this.serializeMedicalEventDate(event.endDate, end, true)
        }).pipe(timeout(15000));
    }

    updateEvent(id: number, event: MedicalEvent): Observable<MedicalEvent> {
        return this.http.put<MedicalEvent>(`${this.eventUrl}/${id}`, {
            title: event.title,
            description: event.description ?? '',
            type: event.type,
            difficulty: event.difficulty,
            status: event.status,
            patientId: event.patientId,
            familyId: event.familyId ?? null,
            startDate: this.serializeMedicalEventDate(event.startDate, new Date()),
            endDate: this.serializeMedicalEventDate(event.endDate, new Date(), true)
        });
    }

    deleteEvent(id: number): Observable<void> {
        return this.http.delete<void>(`${this.eventUrl}/${id}`);
    }

    searchRecommendations(query: string): Observable<Recommendation[]> {
        return this.http.get<Recommendation[]>(`${this.apiUrl}/search?query=${query}`);
    }

    generateAutomaticRecommendations(payload: AutoRecommendationGenerateRequest): Observable<Recommendation[]> {
        return this.http.post<Recommendation[]>(`${this.apiUrl}/auto-generate`, payload).pipe(timeout(15000));
    }

    getDoctorAlerts(doctorId: number): Observable<ClinicalEscalationAlert[]> {
        return this.http.get<ClinicalEscalationAlert[]>(`${this.apiUrl}/doctor/${doctorId}/alerts`).pipe(timeout(15000));
    }

    resolveAlert(alertId: number): Observable<ClinicalEscalationAlert> {
        return this.http.patch<ClinicalEscalationAlert>(`${this.apiUrl}/alerts/${alertId}/resolve`, {}).pipe(timeout(15000));
    }

    searchEvents(query: string): Observable<MedicalEvent[]> {
        return this.http.get<MedicalEvent[]>(`${this.eventUrl}/search?query=${query}`);
    }

    getActiveEventsByPatient(patientId: number): Observable<MedicalEvent[]> {
        return this.http.get<MedicalEvent[]>(`${this.eventUrl}/patient/${patientId}/active`);
    }

    getCompletedEventsByPatient(patientId: number): Observable<MedicalEvent[]> {
        return this.http.get<MedicalEvent[]>(`${this.eventUrl}/patient/${patientId}/completed`);
    }

    getEventsByType(type: MedicalEventType): Observable<MedicalEvent[]> {
        return this.http.get<MedicalEvent[]>(`${this.eventUrl}/type/${type}`);
    }

    joinMedicalEvent(eventId: number, request: JoinMedicalEventRequest): Observable<MedicalEventParticipation> {
        return this.http.post<MedicalEventParticipation>(`${this.eventUrl}/${eventId}/join`, request);
    }

    getStreak(eventId: number, participantId: number, participantType: ParticipantType): Observable<StreakResponse> {
        return this.http.get<StreakResponse>(`${this.eventUrl}/${eventId}/streak?participantId=${participantId}&participantType=${participantType}`);
    }

    getScore(eventId: number, participantId: number, participantType: ParticipantType): Observable<ScoreResponse> {
        return this.http.get<ScoreResponse>(`${this.eventUrl}/${eventId}/score?participantId=${participantId}&participantType=${participantType}`);
    }

    getRanking(eventId: number): Observable<ParticipantRanking[]> {
        return this.http.get<ParticipantRanking[]>(`${this.eventUrl}/${eventId}/ranking`);
    }

    hasJoined(eventId: number, participantId: number, participantType: ParticipantType): Observable<boolean> {
        return this.http.get<boolean>(`${this.eventUrl}/${eventId}/joined?participantId=${participantId}&participantType=${participantType}`);
    }

    completeExpiredMedicalEvents(): Observable<number> {
        return this.http.post<number>(`${this.eventUrl}/complete-expired`, {});
    }

    getPuzzleById(puzzleId: number): Observable<Puzzle> {
        return this.http.get<Puzzle>(`${this.puzzleUrl}/${puzzleId}`).pipe(timeout(15000));
    }

    createPuzzle(payload: PuzzleCreateRequest): Observable<Puzzle> {
        return this.http.post<Puzzle>(this.puzzleUrl, {
            ...payload,
            title: payload.title?.trim() || null,
            description: payload.description?.trim() || null,
            startDate: payload.startDate || null,
            endDate: payload.endDate || null
        }).pipe(timeout(15000));
    }

    getPuzzlesByPatient(patientId: number): Observable<Puzzle[]> {
        return this.http.get<Puzzle[]>(`${this.puzzleUrl}/patient/${patientId}`).pipe(timeout(15000));
    }

    getPuzzleByEvent(eventId: number): Observable<Puzzle> {
        return this.http.get<Puzzle>(`${this.puzzleUrl}/event/${eventId}`).pipe(timeout(15000));
    }

    startPuzzleSession(puzzleId: number, patientId: number): Observable<PuzzleSessionStartResponse> {
        return this.http
            .post<PuzzleSessionStartResponse>(`${this.puzzleUrl}/${puzzleId}/sessions/start?patientId=${patientId}`, {})
            .pipe(timeout(15000));
    }

    submitPuzzleSession(
        puzzleId: number,
        sessionId: number,
        payload: PuzzleSessionSubmitRequest
    ): Observable<PuzzleSession> {
        return this.http
            .post<PuzzleSession>(`${this.puzzleUrl}/${puzzleId}/sessions/${sessionId}/submit`, payload)
            .pipe(timeout(15000));
    }

    getPuzzleSessions(puzzleId: number, patientId: number): Observable<PuzzleSession[]> {
        return this.http
            .get<PuzzleSession[]>(`${this.puzzleUrl}/${puzzleId}/sessions/patient/${patientId}`)
            .pipe(timeout(15000));
    }

    getPuzzleLeaderboard(puzzleId: number): Observable<PuzzleLeaderboardEntry[]> {
        return this.http.get<PuzzleLeaderboardEntry[]>(`${this.puzzleUrl}/${puzzleId}/leaderboard`).pipe(timeout(15000));
    }

    getSudokuByEvent(eventId: number): Observable<SudokuGame> {
        return this.http.get<SudokuGame>(`${this.sudokuUrl}/event/${eventId}`).pipe(timeout(15000));
    }

    startSudokuSession(gameId: number, patientId: number): Observable<SudokuSessionStartResponse> {
        return this.http
            .post<SudokuSessionStartResponse>(`${this.sudokuUrl}/${gameId}/sessions/start?patientId=${patientId}`, {})
            .pipe(timeout(15000));
    }

    submitSudokuSession(
        gameId: number,
        sessionId: number,
        payload: SudokuSessionSubmitRequest
    ): Observable<SudokuSessionResponse> {
        return this.http
            .post<SudokuSessionResponse>(`${this.sudokuUrl}/${gameId}/sessions/${sessionId}/submit`, payload)
            .pipe(timeout(15000));
    }

    getSudokuSessions(gameId: number, patientId: number): Observable<SudokuSessionResponse[]> {
        return this.http
            .get<SudokuSessionResponse[]>(`${this.sudokuUrl}/${gameId}/sessions?patientId=${patientId}`)
            .pipe(timeout(15000));
    }

    createSudoku(payload: { patientId: number; difficulty: string; timeLimitSeconds: number; title?: string }): Observable<SudokuGame> {
        return this.http.post<SudokuGame>(this.sudokuUrl, payload).pipe(timeout(15000));
    }

    private recommendationApiBase(): string {
        if (!environment.production && environment.gatewayBaseUrl) {
            return environment.gatewayBaseUrl.replace(/\/$/, '');
        }
        return environment.apiUrl.replace(/\/$/, '');
    }

    private serializeMedicalEventDate(value: string | undefined, fallback: Date, endOfDay = false): string {
        const trimmedValue = value?.trim();
        if (!trimmedValue) {
            return this.toLocalDateTimeString(fallback);
        }

        if (/^\d{4}-\d{2}-\d{2}$/.test(trimmedValue)) {
            return `${trimmedValue}T${endOfDay ? '23:59:59' : '00:00:00'}`;
        }

        const parsedDate = new Date(trimmedValue);
        if (!Number.isNaN(parsedDate.getTime())) {
            return this.toLocalDateTimeString(parsedDate);
        }

        return trimmedValue;
    }

    private toLocalDateTimeString(date: Date): string {
        const pad = (value: number) => String(value).padStart(2, '0');
        return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
            + `T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
    }
}

