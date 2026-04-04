export interface IncidentType {
  id: number;
  name: string;
  description?: string;
  defaultSeverity?: SeverityLevel;
  points?: number;
}

export enum SeverityLevel {
  LOW = 'LOW',
  MEDIUM = 'MEDIUM',
  HIGH = 'HIGH',
  CRITICAL = 'CRITICAL',
}

export enum IncidentStatus {
  OPEN = 'OPEN',
  IN_PROGRESS = 'IN_PROGRESS',
  RESOLVED = 'RESOLVED',
}

export interface IncidentComment {
  id?: number;
  incidentId?: number;
  content: string;
  authorName?: string;
  createdAt?: string;
}

export interface Incident {
  id?: number;
  description?: string;
  severityLevel?: SeverityLevel | 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  status?: IncidentStatus | string;
  incidentDate?: string;
  source?: string;
  patientId?: number;
  caregiverId?: number;
  computedScore?: number;
  type: IncidentType;
}
