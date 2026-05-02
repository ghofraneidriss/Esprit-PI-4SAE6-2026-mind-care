import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  LostItem,
  SearchReport,
  ItemStatus,
  ItemCategory,
  ItemAlert,
  GlobalStats,
  PatientItemRisk,
  FrequentLosingTrend,
  RecoveryStrategy,
  SearchSuggestion,
} from './lost-item.model';

@Injectable({ providedIn: 'root' })
export class LostItemService {
  private itemsUrl = 'http://localhost:8080/api/lost-items';
  private reportsUrl = 'http://localhost:8080/api/search-reports';
  private alertsUrl = 'http://localhost:8080/api/item-alerts';
  private statsUrl = 'http://localhost:8080/api/lost-items';

  constructor(private http: HttpClient) {}

  // ── Lost Items ──────────────────────────────────────────────────────────────

  getAllItems(): Observable<LostItem[]> {
    return this.http.get<LostItem[]>(this.itemsUrl);
  }

  getItemById(id: number): Observable<LostItem> {
    return this.http.get<LostItem>(`${this.itemsUrl}/${id}`);
  }

  getPatientItems(patientId: number, status?: ItemStatus): Observable<LostItem[]> {
    let params = new HttpParams().set('patientId', patientId.toString());
    if (status) params = params.set('status', status);
    return this.http.get<LostItem[]>(this.itemsUrl, { params });
  }

  createItem(item: Partial<LostItem>): Observable<LostItem> {
    return this.http.post<LostItem>(this.itemsUrl, item);
  }

  updateItem(id: number, item: Partial<LostItem>): Observable<LostItem> {
    return this.http.put<LostItem>(`${this.itemsUrl}/${id}`, item);
  }

  deleteItem(id: number): Observable<void> {
    return this.http.delete<void>(`${this.itemsUrl}/${id}`);
  }

  markAsFound(id: number): Observable<LostItem> {
    return this.http.patch<LostItem>(`${this.itemsUrl}/${id}/mark-found`, {});
  }

  // ── Search Reports ──────────────────────────────────────────────────────────

  getAllReports(): Observable<SearchReport[]> {
    return this.http.get<SearchReport[]>(this.reportsUrl);
  }

  getReportsByItem(itemId: number): Observable<SearchReport[]> {
    return this.http.get<SearchReport[]>(`${this.reportsUrl}/item/${itemId}`);
  }

  createReport(report: Partial<SearchReport>): Observable<SearchReport> {
    return this.http.post<SearchReport>(this.reportsUrl, report);
  }

  // ── Alerts ──────────────────────────────────────────────────────────────────

  getAlerts(): Observable<ItemAlert[]> {
    return this.http.get<ItemAlert[]>(this.alertsUrl);
  }

  getItemAlerts(itemId: number): Observable<ItemAlert[]> {
    return this.http.get<ItemAlert[]>(`${this.alertsUrl}/item/${itemId}`);
  }

  resolveAlert(alertId: number): Observable<ItemAlert> {
    return this.http.patch<ItemAlert>(`${this.alertsUrl}/${alertId}/resolve`, {});
  }

  // ── Statistics & Analytics ──────────────────────────────────────────────────

  getGlobalStats(): Observable<GlobalStats> {
    return this.http.get<GlobalStats>(`${this.itemsUrl}/statistics`);
  }

  getPatientItemRisk(patientId: number): Observable<PatientItemRisk> {
    return this.http.get<PatientItemRisk>(`${this.itemsUrl}/patient/${patientId}/risk`);
  }

  getFrequentLosingTrend(patientId: number): Observable<FrequentLosingTrend> {
    return this.http.get<FrequentLosingTrend>(`${this.itemsUrl}/patient/${patientId}/trend`);
  }

  getRecoveryStrategy(itemId: number): Observable<RecoveryStrategy> {
    return this.http.get<RecoveryStrategy>(`${this.itemsUrl}/${itemId}/recovery-strategy`);
  }

  getSearchSuggestions(itemId: number): Observable<SearchSuggestion[]> {
    return this.http.get<SearchSuggestion[]>(`${this.itemsUrl}/${itemId}/suggestions`);
  }

  getCriticalItems(): Observable<LostItem[]> {
    return this.http.get<LostItem[]>(`${this.itemsUrl}/critical/all`);
  }
}
