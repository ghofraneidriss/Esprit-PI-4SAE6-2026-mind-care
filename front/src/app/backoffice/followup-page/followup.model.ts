export type MoodState = 'CALM' | 'HAPPY' | 'ANXIOUS' | 'AGITATED' | 'DEPRESSED' | 'CONFUSED';
export type SleepQuality = 'EXCELLENT' | 'GOOD' | 'FAIR' | 'POOR';
export type IndependenceLevel = 'INDEPENDENT' | 'NEEDS_ASSISTANCE' | 'DEPENDENT';

export interface FollowUp {
  id?: number;
  patientId: number;
  caregiverId: number;
  followUpDate: string;
  cognitiveScore?: number;
  mood?: MoodState;
  agitationObserved?: boolean;
  confusionObserved?: boolean;
  eating?: IndependenceLevel;
  dressing?: IndependenceLevel;
  mobility?: IndependenceLevel;
  hoursSlept?: number;
  sleepQuality?: SleepQuality;
  notes?: string;
  vitalSigns?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface FollowUpStats {
  totalFollowUps: number;
  averageCognitiveScore: number;
  averageHoursSlept: number;
  moodDistribution: Record<string, number>;
  sleepQualityDistribution: Record<string, number>;
  agitationCount: number;
  confusionCount: number;
  lowCognitiveCount: number;
  poorSleepCount: number;
}

export interface PatientRisk {
  patientId: number;
  riskLevel: 'LOW' | 'MODERATE' | 'HIGH' | 'CRITICAL';
  riskScore: number;
  riskFactors: string[];
  totalFollowUps: number;
  unresolvedAlerts: number;
  latestCognitiveScore?: number;
  latestMood?: MoodState;
  latestSleepQuality?: SleepQuality;
}

export interface CognitiveDeclineResult {
  patientId: number;
  cognitiveDecline: boolean;
}
