export type ReclamationStatus = 'OPEN' | 'IN_PROGRESS' | 'RESOLVED' | 'REJECTED';
export type ReclamationClassificationLevel = 'FAIBLE' | 'MOYEN' | 'FORT';

export interface Reclamation {
  id?: number;
  userName: string;
  userEmail: string;
  subject: string;
  imageUrl?: string;
  rating: number;
  description: string;
  status: ReclamationStatus;
  classificationLevel?: ReclamationClassificationLevel;
  classificationScore?: number;
  classificationKeywords?: string;
  classificationReason?: string;
  classifiedBy?: string;
  classifiedAt?: string;
  createdAt?: string;
}
