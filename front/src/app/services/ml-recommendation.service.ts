import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface PatientInput {
  Age: number; Gender: number; Ethnicity: number; EducationLevel: number;
  BMI: number; Smoking: number; AlcoholConsumption: number;
  PhysicalActivity: number; DietQuality: number; SleepQuality: number;
  FamilyHistoryAlzheimers: number; CardiovascularDisease: number;
  Diabetes: number; Depression: number; HeadInjury: number; Hypertension: number;
  SystolicBP: number; DiastolicBP: number; CholesterolTotal: number;
  CholesterolLDL: number; CholesterolHDL: number; CholesterolTriglycerides: number;
  MMSE: number; FunctionalAssessment: number; MemoryComplaints: number;
  BehavioralProblems: number; ADL: number;
  Confusion: number; Disorientation: number; PersonalityChanges: number;
  DifficultyCompletingTasks: number; Forgetfulness: number;
}

export interface Recommendation {
  feature: string;
  category: string;
  priority: 'High' | 'Medium' | 'Low';
  recommendation: string;
  detail: string;
  contribution_score: number;
  patient_value: number;
  healthy_avg: number;
}

export interface ClusterProfile {
  label: string; size: number;
  avg_mmse: number; avg_adl: number; avg_functional: number;
  avg_physical_activity: number; avg_diet_quality: number; avg_sleep_quality: number;
  avg_bmi: number; avg_systolic_bp: number; avg_cholesterol_ldl: number;
  diagnosis_rate: number;
}

export interface RecommendationResult {
  risk_score: number;
  risk_label: string;
  cluster_id: number;
  cluster_label: string;
  cluster_profile: ClusterProfile;
  prob_rf: number;
  prob_gb: number;
  total_recommendations: number;
  risk_drivers: { feature: string; contribution: number; patient_value: number; healthy_avg: number }[];
  recommendations: Recommendation[];
  model_info: { rf_cv_auc: number; gb_cv_auc: number; best_model: string };
}

@Injectable({ providedIn: 'root' })
export class MlRecommendationService {
  private readonly baseUrl = 'http://localhost:5000';

  constructor(private http: HttpClient) {}

  getRecommendations(patient: PatientInput): Observable<RecommendationResult> {
    return this.http.post<RecommendationResult>(`${this.baseUrl}/recommend`, patient);
  }

  getClusters(): Observable<any> {
    return this.http.get<any>(`${this.baseUrl}/clusters`);
  }
}
