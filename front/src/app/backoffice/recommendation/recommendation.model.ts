export enum RecommendationType {
    MEDICINE = 'MEDICINE',
    EXERCISE = 'EXERCISE',
    DIET = 'DIET',
    LIFESTYLE = 'LIFESTYLE',
    OTHER = 'OTHER'
}

export enum RecommendationStatus {
    PENDING = 'PENDING',
    APPROVED = 'APPROVED',
    REJECTED = 'REJECTED'
}

export interface MedicalEvent {
    id?: number;
    title: string;
    description: string;
    type: string;
    difficulty: string;
}

export interface Recommendation {
    id?: number;
    content: string;
    type: RecommendationType;
    status: RecommendationStatus;

    // Microservices tracking
    doctorId: number;
    patientId: number;
    doctorName?: string;
    patientName?: string;

    // Medical Games / Events
    medicalEvents?: MedicalEvent[];

    createdAt?: string;
}
