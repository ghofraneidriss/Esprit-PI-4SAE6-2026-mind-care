export type FileType = 'MRI_SCAN' | 'PDF_REPORT' | 'IMAGE' | 'OTHER';

export interface FileRecord {
  fileid?: number;
  caregiverid: number | null;
  fileName: string;
  fileType: FileType;
  createdAt?: string | null;
  updatedAt?: string | null;
}
