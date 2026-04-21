import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

/**
 * Service pour la gestion des ordonnances (Prescriptions) côté Médecin.
 * Interagit avec le microservice ordonnance_et_medicaments (port 8083).
 */
@Injectable({
  providedIn: 'root'
})
export class DoctorPrescriptionService {
  private apiUrl = 'http://localhost:8083/api/doctor';
  private readonly adminMedicineApiUrl = 'http://localhost:8083/api/admin/medicines';
  private readonly suggestionApiUrl = 'http://localhost:8083/api/medicaments';

  constructor(private http: HttpClient) { }

  // Gestion des médicaments (utilisé par le médecin pour chercher)
  searchMedicines(query: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/prescriptions/search-medicines?query=${query}`);
  }

  getAllMedicines(): Observable<any[]> {
    return this.http.get<any[]>(this.adminMedicineApiUrl);
  }

  suggestMedicineNames(query: string): Observable<string[]> {
    return this.http.get<string[]>(`${this.suggestionApiUrl}/suggest-names?query=${query}`);
  }

  suggestMedicineCategories(query: string): Observable<string[]> {
    return this.http.get<string[]>(`${this.suggestionApiUrl}/suggest-categories?query=${query}`);
  }

  // Gestion des prescriptions
  getHistoryByPatient(patientId: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/prescriptions/patient/${patientId}`);
  }

  /**
   * JPQL — Vérifie si un médicament est déjà prescrit au patient sur des dates qui se chevauchent.
   * Appelle GET /api/doctor/prescriptions/check-overlap
   * @returns Liste des conflits détectés (vide = aucun conflit)
   */
  checkMedicineOverlap(patientId: number, medicineId: number,
                       startDate: string, endDate: string,
                       currentPrescriptionId: number = 0): Observable<any[]> {
    const params = `patientId=${patientId}&medicineId=${medicineId}`
                 + `&startDate=${startDate}&endDate=${endDate}`
                 + `&currentPrescriptionId=${currentPrescriptionId}`;
    return this.http.get<any[]>(`${this.apiUrl}/prescriptions/check-overlap?${params}`);
  }

  /**
   * JPQL — Détecte le comportement de "Doctor Shopping".
   * Vérifie si le patient possède déjà une prescription active (endDate >= aujourd'hui)
   * pour ce médicament, prescrite par un médecin DIFFÉRENT du médecin courant.
   * Si la prescription est expirée, aucune alerte n'est retournée.
   * Appelle GET /api/doctor/prescriptions/check-doctor-shopping
   * @returns Liste d'alertes de doctor shopping (vide = aucun comportement suspect)
   */
  checkDoctorShopping(patientId: number, medicineId: number, currentDoctorId: number): Observable<any[]> {
    const params = `patientId=${patientId}&medicineId=${medicineId}&currentDoctorId=${currentDoctorId}`;
    return this.http.get<any[]>(`${this.apiUrl}/prescriptions/check-doctor-shopping?${params}`);
  }

  checkDrugSafety(
    patientId: number,
    medicineId: number,
    startDate: string,
    endDate: string,
    currentPrescriptionId: number = 0
  ): Observable<any[]> {
    const params =
      `patientId=${patientId}&medicineId=${medicineId}` +
      `&startDate=${startDate}&endDate=${endDate}` +
      `&currentPrescriptionId=${currentPrescriptionId}`;
    return this.http.get<any[]>(`${this.apiUrl}/prescriptions/check-drug-safety?${params}`);
  }

  createPrescription(prescription: any): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/prescriptions`, prescription);
  }

  saveDraftPrescription(prescription: any): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/prescriptions/draft`, prescription);
  }

  updatePrescription(id: number, prescription: any): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/prescriptions/${id}`, prescription);
  }

  deletePrescription(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/prescriptions/${id}`);
  }

  getPrescriptionById(id: number): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/prescriptions/${id}`);
  }

  // Brouillon (Draft) Management
  private readonly DRAFT_KEY = 'doctor_prescription_draft';

  saveDraft(draft: any): void {
    localStorage.setItem(this.DRAFT_KEY, JSON.stringify(draft));
  }

  getDraft(): any | null {
    const draft = localStorage.getItem(this.DRAFT_KEY);
    return draft ? JSON.parse(draft) : null;
  }

  clearDraft(): void {
    localStorage.removeItem(this.DRAFT_KEY);
  }
}
