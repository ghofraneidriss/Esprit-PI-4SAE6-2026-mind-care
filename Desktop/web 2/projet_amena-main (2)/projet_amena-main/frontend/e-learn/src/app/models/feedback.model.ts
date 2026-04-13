export interface Feedback {
  id?: number;
  userName: string;
  userEmail: string;
  title: string;
  imageUrl?: string;
  rating: number;
  comment: string;
  moderationStatus?: 'PENDING' | 'APPROVED' | 'REJECTED';
  moderationScore?: number;
  moderationFlagged?: boolean;
  blockedWords?: string;
  moderationNote?: string;
  autoReclamationCreated?: boolean;
  linkedReclamationId?: number;
  autoReclamationStatus?: string;
  createdAt?: string;
}
