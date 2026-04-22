export enum ThemeCulturel {
    MUSIQUE = 'MUSIQUE',
    CINEMA = 'CINEMA',
    GASTRONOMIE = 'GASTRONOMIE',
    TRADITIONS = 'TRADITIONS',
    VOYAGE = 'VOYAGE',
    ART = 'ART',
    SPORT = 'SPORT',
    HISTOIRE = 'HISTOIRE',
    AUTRE = 'AUTRE'
}

export enum MediaType {
    IMAGE = 'IMAGE',
    AUDIO = 'AUDIO'
}

export interface EntreeSouvenir {
    id?: number;
    patientId: number;
    doctorId: number;
    caregiverId: number;
    infosCaregiver?: string | null;
    texte: string;
    mediaType: MediaType;
    mediaUrl?: string | null;
    mediaTitle?: string | null;
    expectedSpeakerName?: string | null;
    expectedSpeakerRelation?: string | null;
    patientGuessSpeakerName?: string | null;
    voiceRecognized?: boolean;
    themeCulturel: ThemeCulturel;
    traitee?: boolean;
    createdAt?: string;
    updatedAt?: string;
}

export interface EntreeCountResponse {
    patientId: number;
    totalEntrees: number;
}

export interface VoiceRecognitionRequest {
    patientGuessSpeakerName: string;
    voiceRecognized?: boolean;
}
