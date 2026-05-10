export enum AlzheimerStage {
    PRECLINICAL = 'PRECLINICAL',
    MILD = 'MILD',
    MODERATE = 'MODERATE',
    SEVERE = 'SEVERE'
}

export interface Consultation {
    id?: number;
    appointmentId: number;
    clinicalNotes: string;
    currentWeight: number;
    bloodPressure: string;
    mmseScore: number;
    alzheimerStage: AlzheimerStage;
    diseaseProbability?: number;
    isSick?: boolean;
    mlCluster?: number;
}
