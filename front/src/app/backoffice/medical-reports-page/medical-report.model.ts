export type ReportStatus = 'DRAFT' | 'REVIEWED' | 'APPROVED';

export interface File {
  fileid?: number;
  fileName: string;
  fileType: string;
  createdAt: string;
}

export interface MedicalReport {
  reportid?: number;
  patientid: number | null;
  doctorid: number | null;
  patientName?: string;
  doctorName?: string;
  reportUrl?: string | null;
  doctorEmail?: string | null;
  status: ReportStatus;
  createdAt?: string | null;
  title: string;
  description: string;
  approvalByDocter?: number | null;
  approvedAt?: string | null;
  files?: File[];
}
