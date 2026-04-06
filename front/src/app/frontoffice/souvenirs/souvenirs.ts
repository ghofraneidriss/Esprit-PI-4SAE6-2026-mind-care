import { Component, OnInit } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import {
    EntreeSouvenir,
    MediaType,
    ThemeCulturel
} from '../../backoffice/souvenirs/souvenirs.model';
import { SouvenirsService } from '../../backoffice/souvenirs/souvenirs.service';
import { AuthService, AuthUser } from '../auth/auth.service';

@Component({
    selector: 'app-front-souvenirs',
    templateUrl: './souvenirs.html',
    styleUrls: ['./souvenirs.css'],
    standalone: false
})
export class SouvenirsFrontPage implements OnInit {
    currentPatient: AuthUser | null = null;
    themeFilter: ThemeCulturel | '' = '';
    mediaTypeFilter: MediaType | '' = '';

    readonly themeOptions = Object.values(ThemeCulturel);
    readonly mediaTypeOptions = Object.values(MediaType);

    entrees: EntreeSouvenir[] = [];
    selectedEntree: EntreeSouvenir | null = null;

    totalEntrees = 0;
    loadingEntrees = false;
    savingVoiceAnswer = false;
    errorMessage = '';
    successMessage = '';

    voiceGuess = '';
    voiceRecognized = true;

    constructor(
        private readonly souvenirsService: SouvenirsService,
        private readonly authService: AuthService
    ) { }

    ngOnInit(): void {
        this.currentPatient = this.authService.getLoggedUser();
        this.loadSouvenirs();
    }

    get isPatientLoggedIn(): boolean {
        return this.currentPatient !== null && this.authService.isPatient();
    }

    loadSouvenirs(): void {
        if (!this.isPatientLoggedIn || !this.currentPatient?.userId) {
            this.errorMessage = this.currentPatient
                ? 'Cette page patient affiche uniquement les souvenirs du patient connecte.'
                : 'Connectez-vous comme patient pour voir vos souvenirs.';
            this.entrees = [];
            this.totalEntrees = 0;
            this.selectedEntree = null;
            this.loadingEntrees = false;
            return;
        }

        this.loadingEntrees = true;
        this.errorMessage = '';
        this.successMessage = '';

        this.souvenirsService.getEntreesByPatient(
            this.currentPatient.userId,
            this.themeFilter || undefined,
            this.mediaTypeFilter || undefined
        ).subscribe({
            next: (data) => {
                this.entrees = data;
                this.loadingEntrees = false;
                this.totalEntrees = data.length;
                this.selectedEntree = data.length > 0 ? data[0] : null;
                this.syncVoiceFields();
            },
            error: (err: unknown) => {
                this.errorMessage = this.extractErrorMessage(err, 'Erreur de chargement de vos souvenirs.');
                this.loadingEntrees = false;
                this.entrees = [];
                this.totalEntrees = 0;
                this.selectedEntree = null;
            }
        });
    }

    selectEntree(entree: EntreeSouvenir): void {
        this.selectedEntree = entree;
        this.syncVoiceFields();
    }

    clearFilters(): void {
        this.themeFilter = '';
        this.mediaTypeFilter = '';
        this.loadSouvenirs();
    }

    submitVoiceRecognition(): void {
        if (!this.selectedEntree?.id) {
            this.errorMessage = 'Selectionnez d abord un souvenir audio.';
            return;
        }
        if (this.selectedEntree.mediaType !== MediaType.AUDIO) {
            this.errorMessage = 'La reconnaissance de voix est reservee aux souvenirs audio.';
            return;
        }
        if (!this.voiceGuess.trim()) {
            this.errorMessage = 'Veuillez saisir le nom de la personne reconnue.';
            return;
        }

        this.savingVoiceAnswer = true;
        this.errorMessage = '';

        this.souvenirsService.updateVoiceRecognition(this.selectedEntree.id, {
            patientGuessSpeakerName: this.voiceGuess.trim(),
            voiceRecognized: this.voiceRecognized
        }).subscribe({
            next: (updated) => {
                this.savingVoiceAnswer = false;
                this.successMessage = 'Votre reponse a bien ete enregistree.';
                this.selectedEntree = updated;
                this.entrees = this.entrees.map((entry) => entry.id === updated.id ? updated : entry);
                this.syncVoiceFields();
            },
            error: (err: unknown) => {
                this.savingVoiceAnswer = false;
                this.errorMessage = this.extractErrorMessage(err, 'Erreur lors de l enregistrement de votre reponse vocale.');
            }
        });
    }

    private syncVoiceFields(): void {
        this.voiceGuess = this.selectedEntree?.patientGuessSpeakerName ?? '';
        this.voiceRecognized = this.selectedEntree?.voiceRecognized ?? true;
    }

    private extractErrorMessage(err: unknown, fallback: string): string {
        if (err instanceof HttpErrorResponse) {
            if (typeof err.error === 'string') return err.error;
            if (err.error?.message) return err.error.message;
            if (err.message) return err.message;
        }
        return fallback;
    }
}
