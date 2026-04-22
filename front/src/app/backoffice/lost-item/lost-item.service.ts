import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  LostItem, SearchReport, ItemStatus, ItemCategory, PagedLostItems,
  LostItemAlert, AlertLevel, AlertStatus,
  GlobalStats, AlertStats, PatientRisk, FrequencyTrend,
  SearchTimeline, SearchLogStats, RecoveryStrategy,
  PatientIntelligence, SearchSuggestion
} from './lost-item.model';

@Injectable({ providedIn: 'root' })
export class LostItemService {
  private readonly itemsUrl   = 'http://localhost:8086/api/lost-items';
  private readonly reportsUrl = 'http://localhost:8086/api/search-reports';
  private readonly alertsUrl  = 'http://localhost:8086/api/item-alerts';

  constructor(private readonly http: HttpClient) {}

  // ── Lost Items ──────────────────────────────────────────────────────────────

  getAllLostItems(): Observable<LostItem[]> {
    return this.http.get<LostItem[]>(this.itemsUrl);
  }

  createLostItem(item: Partial<LostItem>): Observable<LostItem> {
    return this.http.post<LostItem>(this.itemsUrl, item);
  }

  getLostItemById(id: number): Observable<LostItem> {
    return this.http.get<LostItem>(`${this.itemsUrl}/${id}`);
  }

  getPatientLostItems(
    patientId: number,
    status?: ItemStatus | '',
    category?: ItemCategory | '',
    page = 0,
    size = 20
  ): Observable<PagedLostItems> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (status)   params = params.set('status', status);
    if (category) params = params.set('category', category);
    return this.http.get<PagedLostItems>(`${this.itemsUrl}/patient/${patientId}`, { params });
  }

  updateLostItem(id: number, item: Partial<LostItem>): Observable<LostItem> {
    return this.http.put<LostItem>(`${this.itemsUrl}/${id}`, item);
  }

  deleteLostItem(id: number): Observable<any> {
    return this.http.delete(`${this.itemsUrl}/${id}`);
  }

  markAsFound(id: number): Observable<LostItem> {
    return this.http.patch<LostItem>(`${this.itemsUrl}/${id}/mark-found`, {});
  }

  getCriticalLostItems(patientId: number): Observable<{ items: LostItem[]; urgentCount: number }> {
    return this.http.get<{ items: LostItem[]; urgentCount: number }>(
      `${this.itemsUrl}/patient/${patientId}/critical`
    );
  }

  getAllCriticalItems(): Observable<{ items: LostItem[]; urgentCount: number }> {
    return this.http.get<{ items: LostItem[]; urgentCount: number }>(`${this.itemsUrl}/critical/all`);
  }

  getItemsByCaregiverId(caregiverId: number): Observable<LostItem[]> {
    return this.http.get<LostItem[]>(`${this.itemsUrl}/caregiver/${caregiverId}`);
  }

  getCriticalItemsByCaregiverId(caregiverId: number): Observable<{ items: LostItem[]; urgentCount: number }> {
    return this.http.get<{ items: LostItem[]; urgentCount: number }>(
      `${this.itemsUrl}/caregiver/${caregiverId}/critical`
    );
  }

  getAlertsByCaregiverId(caregiverId: number): Observable<LostItemAlert[]> {
    return this.http.get<LostItemAlert[]>(`${this.alertsUrl}/caregiver/${caregiverId}`);
  }

  // ── Advanced: Statistics, Risk, Trend ──────────────────────────────────────

  getGlobalStatistics(): Observable<GlobalStats> {
    return this.http.get<GlobalStats>(`${this.itemsUrl}/statistics`);
  }

  getPatientItemRisk(patientId: number): Observable<PatientRisk> {
    return this.http.get<PatientRisk>(`${this.itemsUrl}/patient/${patientId}/risk`);
  }

  getPatientFrequencyTrend(patientId: number): Observable<FrequencyTrend> {
    return this.http.get<FrequencyTrend>(`${this.itemsUrl}/patient/${patientId}/trend`);
  }

  // ── Search Reports ──────────────────────────────────────────────────────────

  createSearchReport(report: Partial<SearchReport>): Observable<SearchReport> {
    return this.http.post<SearchReport>(this.reportsUrl, report);
  }

  getSearchReportsByLostItemId(lostItemId: number): Observable<SearchReport[]> {
    return this.http.get<SearchReport[]>(`${this.reportsUrl}/lost-item/${lostItemId}`);
  }

  getSearchReportById(id: number): Observable<SearchReport> {
    return this.http.get<SearchReport>(`${this.reportsUrl}/${id}`);
  }

  updateSearchReport(id: number, report: Partial<SearchReport>): Observable<SearchReport> {
    return this.http.put<SearchReport>(`${this.reportsUrl}/${id}`, report);
  }

  deleteSearchReport(id: number): Observable<any> {
    return this.http.delete(`${this.reportsUrl}/${id}`);
  }

  getOpenReportsCount(lostItemId: number): Observable<{ lostItemId: number; openCount: number }> {
    return this.http.get<{ lostItemId: number; openCount: number }>(
      `${this.reportsUrl}/lost-item/${lostItemId}/open-count`
    );
  }

  // ── Advanced Search Log ─────────────────────────────────────────────────────

  advancedSearchReports(params: {
    lostItemId?: number;
    reportedBy?: number;
    searchResult?: string;
    status?: string;
    locationKeyword?: string;
    from?: string;
    to?: string;
  }): Observable<SearchReport[]> {
    let httpParams = new HttpParams();
    if (params.lostItemId)     httpParams = httpParams.set('lostItemId', params.lostItemId);
    if (params.reportedBy)     httpParams = httpParams.set('reportedBy', params.reportedBy);
    if (params.searchResult)   httpParams = httpParams.set('searchResult', params.searchResult);
    if (params.status)         httpParams = httpParams.set('status', params.status);
    if (params.locationKeyword) httpParams = httpParams.set('locationKeyword', params.locationKeyword);
    if (params.from)           httpParams = httpParams.set('from', params.from);
    if (params.to)             httpParams = httpParams.set('to', params.to);
    return this.http.get<SearchReport[]>(`${this.reportsUrl}/search`, { params: httpParams });
  }

  getSearchTimeline(lostItemId: number): Observable<SearchTimeline> {
    return this.http.get<SearchTimeline>(`${this.reportsUrl}/lost-item/${lostItemId}/timeline`);
  }

  getSearchLogStats(): Observable<SearchLogStats> {
    return this.http.get<SearchLogStats>(`${this.reportsUrl}/statistics`);
  }

  getReportsByReporter(reportedBy: number): Observable<SearchReport[]> {
    return this.http.get<SearchReport[]>(`${this.reportsUrl}/reporter/${reportedBy}`);
  }

  getReportsByPatient(patientId: number): Observable<SearchReport[]> {
    return this.http.get<SearchReport[]>(`${this.reportsUrl}/patient/${patientId}`);
  }

  // ── Item Alerts ─────────────────────────────────────────────────────────────

  getAllItemAlerts(): Observable<LostItemAlert[]> {
    return this.http.get<LostItemAlert[]>(this.alertsUrl);
  }

  getItemAlertById(id: number): Observable<LostItemAlert> {
    return this.http.get<LostItemAlert>(`${this.alertsUrl}/${id}`);
  }

  getAlertsByLostItemId(lostItemId: number): Observable<LostItemAlert[]> {
    return this.http.get<LostItemAlert[]>(`${this.alertsUrl}/lost-item/${lostItemId}`);
  }

  getAlertsByPatientId(patientId: number): Observable<LostItemAlert[]> {
    return this.http.get<LostItemAlert[]>(`${this.alertsUrl}/patient/${patientId}`);
  }

  getAlertsByLevel(level: AlertLevel): Observable<LostItemAlert[]> {
    return this.http.get<LostItemAlert[]>(`${this.alertsUrl}/level/${level}`);
  }

  getAlertsByStatus(status: AlertStatus): Observable<LostItemAlert[]> {
    return this.http.get<LostItemAlert[]>(`${this.alertsUrl}/status/${status}`);
  }

  getCriticalNewAlerts(): Observable<LostItemAlert[]> {
    return this.http.get<LostItemAlert[]>(`${this.alertsUrl}/critical/new`);
  }

  createItemAlert(alert: Partial<LostItemAlert>): Observable<LostItemAlert> {
    return this.http.post<LostItemAlert>(this.alertsUrl, alert);
  }

  updateItemAlert(id: number, alert: Partial<LostItemAlert>): Observable<LostItemAlert> {
    return this.http.put<LostItemAlert>(`${this.alertsUrl}/${id}`, alert);
  }

  deleteItemAlert(id: number): Observable<any> {
    return this.http.delete(`${this.alertsUrl}/${id}`);
  }

  markAlertViewed(id: number): Observable<LostItemAlert> {
    return this.http.patch<LostItemAlert>(`${this.alertsUrl}/${id}/view`, {});
  }

  resolveAlert(id: number): Observable<LostItemAlert> {
    return this.http.patch<LostItemAlert>(`${this.alertsUrl}/${id}/resolve`, {});
  }

  escalateAlert(id: number): Observable<LostItemAlert> {
    return this.http.patch<LostItemAlert>(`${this.alertsUrl}/${id}/escalate`, {});
  }

  resolveAllAlertsByLostItem(lostItemId: number): Observable<{ resolved: number; lostItemId: number }> {
    return this.http.patch<{ resolved: number; lostItemId: number }>(
      `${this.alertsUrl}/lost-item/${lostItemId}/resolve-all`, {}
    );
  }

  resolveAllAlertsByPatient(patientId: number): Observable<{ resolved: number; patientId: number }> {
    return this.http.patch<{ resolved: number; patientId: number }>(
      `${this.alertsUrl}/patient/${patientId}/resolve-all`, {}
    );
  }

  getAlertStatistics(): Observable<AlertStats> {
    return this.http.get<AlertStats>(`${this.alertsUrl}/statistics`);
  }

  // ── Recovery Intelligence ───────────────────────────────────────────────────

  getRecoveryStrategy(itemId: number): Observable<RecoveryStrategy> {
    return this.http.get<RecoveryStrategy>(`${this.itemsUrl}/${itemId}/recovery-strategy`);
  }

  // ── Option A: Patient Behavioral Intelligence (AI) ─────────────────────────

  getPatientIntelligence(patientId: number): Observable<PatientIntelligence> {
    return this.http.get<PatientIntelligence>(`${this.itemsUrl}/patients/${patientId}/intelligence`);
  }

  // ── Option B: Smart Search Suggestions ─────────────────────────────────────

  getSearchSuggestions(patientId: number, category?: string): Observable<SearchSuggestion[]> {
    let params = new HttpParams().set('patientId', patientId);
    if (category) params = params.set('category', category);
    return this.http.get<SearchSuggestion[]>(`${this.itemsUrl}/search-suggestions`, { params });
  }
}
