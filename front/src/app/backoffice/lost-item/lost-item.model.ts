export type ItemStatus = 'LOST' | 'FOUND' | 'CLOSED' | 'SEARCHING';
export type ItemPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type ItemCategory = 'CLOTHING' | 'ACCESSORY' | 'DOCUMENT' | 'MEDICATION' | 'ELECTRONIC' | 'OTHER';
export type AlertLevel = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type AlertStatus = 'NEW' | 'VIEWED' | 'RESOLVED';

export interface LostItem {
  id?: number;
  patientId: number;
  caregiverId?: number;
  title: string;
  description: string;
  category: ItemCategory;
  status: ItemStatus;
  priority: ItemPriority;
  lastSeenLocation: string;
  lastSeenDate: string;
  imageUrl?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface SearchReport {
  id?: number;
  lostItemId: number;
  reportDate: string;
  searchArea: string;
  status: string;
  notes: string;
  createdAt?: string;
}

export interface ItemAlert {
  id?: number;
  lostItemId: number;
  patientId: number;
  caregiverId?: number;
  title: string;
  description: string;
  level: AlertLevel;
  status: AlertStatus;
  createdAt?: string;
}

export interface GlobalStats {
  totalItems: number;
  lostCount: number;
  searchingCount: number;
  foundCount: number;
  closedCount: number;
  categoryDistribution: Record<string, number>;
  priorityDistribution: Record<string, number>;
  itemsPerPatient: Record<string, number>;
  recoveryRate: number;
  averageDaysToFind: number;
  criticalUnresolvedAlerts: number;
}

export interface PatientItemRisk {
  patientId: number;
  riskScore: number;
  riskLevel: 'LOW' | 'MODERATE' | 'HIGH' | 'CRITICAL';
  riskFactors: string[];
  totalItems: number;
  activeItems: number;
  foundItems: number;
  unresolvedAlerts: number;
  criticalAlerts: number;
  recoveryRate: number;
  hasMedicationLost: boolean;
}

export interface FrequentLosingTrend {
  patientId: number;
  isFrequentLoser: boolean;
  trend: 'INCREASING' | 'DECREASING' | 'STABLE';
  recentMonthCount: number;
  previousMonthCount: number;
  twoMonthsAgoCount: number;
}

export interface RecoveryStrategy {
  itemId: number;
  strategies: string[];
  estimatedRecoveryTime: number;
  successProbability: number;
}

export interface SearchSuggestion {
  location: string;
  probability: number;
  reason: string;
}
