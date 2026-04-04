export type ItemCategory = 'CLOTHING' | 'ACCESSORY' | 'DOCUMENT' | 'MEDICATION' | 'ELECTRONIC' | 'OTHER';
export type ItemStatus = 'LOST' | 'SEARCHING' | 'FOUND' | 'CLOSED';
export type ItemPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type SearchResultType = 'NOT_FOUND' | 'FOUND' | 'PARTIALLY_FOUND';
export type ReportStatus = 'OPEN' | 'CLOSED' | 'ESCALATED';
export type AlertLevel = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type AlertStatus = 'NEW' | 'VIEWED' | 'RESOLVED';

export interface LostItem {
  id?: number;
  title: string;
  description?: string | null;
  category: ItemCategory;
  patientId: number | null;
  caregiverId?: number | null;
  lastSeenLocation?: string | null;
  lastSeenDate?: string | null;
  status?: ItemStatus;
  priority?: ItemPriority;
  imageUrl?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
}

export interface SearchReport {
  id?: number;
  lostItemId: number | null;
  reportedBy: number | null;
  searchDate: string | null;
  locationSearched?: string | null;
  searchResult?: SearchResultType;
  notes?: string | null;
  status?: ReportStatus;
  createdAt?: string | null;
  updatedAt?: string | null;
}

export interface ItemAlert {
  id?: number;
  lostItemId: number;
  patientId: number;
  caregiverId?: number | null;
  title: string;
  description?: string | null;
  level: AlertLevel;
  status?: AlertStatus;
  createdAt?: string | null;
  viewedAt?: string | null;
}

export interface PagedLostItems {
  content: LostItem[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
  pageSize: number;
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

export interface AlertStats {
  totalAlerts: number;
  newCount: number;
  viewedCount: number;
  resolvedCount: number;
  levelDistribution: Record<string, number>;
  alertsPerPatient: Record<string, number>;
  criticalUnresolvedCount: number;
}

export interface PatientRisk {
  patientId: number;
  riskScore: number;
  riskLevel: string;
  riskFactors: string[];
  totalItems: number;
  activeItems: number;
  foundItems: number;
  unresolvedAlerts: number;
  criticalAlerts: number;
  recoveryRate: number;
  hasMedicationLost: boolean;
}

export interface FrequencyTrend {
  patientId: number;
  isFrequentLoser: boolean;
  trend: 'INCREASING' | 'STABLE' | 'DECREASING';
  recentMonthCount: number;
  previousMonthCount: number;
  twoMonthsAgoCount: number;
}

export interface SearchTimelineDay {
  date: string;
  reportCount: number;
  results: Array<{ id: number; result: string; location: string; status: string }>;
}

export interface SearchTimeline {
  lostItemId: number;
  totalSearches: number;
  foundCount: number;
  partiallyFoundCount: number;
  notFoundCount: number;
  openReports: number;
  successRate: number;
  locationFrequency: Record<string, number>;
  timeline: SearchTimelineDay[];
}

export interface SearchLogStats {
  totalReports: number;
  resultDistribution: Record<string, number>;
  statusDistribution: Record<string, number>;
  globalSuccessRate: number;
  topSearchedItems: Array<{ lostItemId: number; itemTitle: string; searchCount: number }>;
  topReporters: Array<{ reportedBy: number; reportCount: number }>;
}
