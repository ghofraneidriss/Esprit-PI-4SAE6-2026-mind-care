import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import {
    EntreeSouvenir,
    MediaType,
    ThemeCulturel
} from './souvenirs.model';
import { SouvenirsService } from './souvenirs.service';
import { AuthService } from '../../core/services/auth.service';
import { RecommendationService } from '../recommendation/recommendation.service';
import { DifficultyLevel, PuzzleCreateRequest } from '../recommendation/recommendation.model';

interface PuzzleFormState {
    title: string;
    description: string;
    difficulty: DifficultyLevel;
    timeLimitSeconds: number;
    maxHints: number;
}

@Component({
    selector: 'app-souvenirs',
    templateUrl: './souvenirs.html',
    styleUrls: ['./souvenirs.css'],
    standalone: false
})
export class SouvenirsPage implements OnInit {
    entrees: EntreeSouvenir[] = [];
    filteredEntrees: EntreeSouvenir[] = [];

    isLoadingEntrees = false;
    isSavingEntree = false;
    isCreatingPuzzle = false;

    showEntreeForm = false;
    showPuzzleForm = false;
    isEditingEntree = false;
    editingEntreeId: number | null = null;
    selectedEntree: EntreeSouvenir | null = null;
    puzzleSourceEntree: EntreeSouvenir | null = null;

    patientFilterInput = '4';
    selectedPatientId: number | null = null;
    themeFilter: ThemeCulturel | '' = '';
    mediaTypeFilter: MediaType | '' = '';
    searchTerm = '';

    totalEntrees = 0;

    errorMessage = '';
    successMessage = '';

    readonly themeOptions = Object.values(ThemeCulturel);
    readonly mediaTypeOptions = Object.values(MediaType);
    readonly puzzleDifficultyOptions = Object.values(DifficultyLevel);

    entreeForm: EntreeSouvenir = this.initEntreeForm();
    puzzleForm: PuzzleFormState = this.initPuzzleForm();

    constructor(
        private readonly souvenirsService: SouvenirsService,
        private readonly recommendationService: RecommendationService,
        private readonly cdr: ChangeDetectorRef,
        public readonly authService: AuthService
    ) { }

    ngOnInit(): void {
        this.loadByPatient();
    }

    get imageCount(): number {
        return this.entrees.filter((entree) => entree.mediaType === MediaType.IMAGE).length;
    }

    get audioCount(): number {
        return this.entrees.filter((entree) => entree.mediaType === MediaType.AUDIO).length;
    }

    get treatedCount(): number {
        return this.entrees.filter((entree) => entree.traitee).length;
    }

    loadByPatient(preferredSelectionId?: number | null): void {
        const patientId = Number(this.patientFilterInput);
        if (!Number.isFinite(patientId) || patientId <= 0) {
            this.errorMessage = 'Donne un patientId valide pour charger les souvenirs.';
            return;
        }

        this.selectedPatientId = patientId;
        this.errorMessage = '';
        this.isLoadingEntrees = true;

        this.souvenirsService.getEntreesByPatient(
            patientId,
            this.themeFilter || undefined,
            this.mediaTypeFilter || undefined
        ).subscribe({
            next: (data) => {
                this.entrees = data;
                this.applyFilters(preferredSelectionId);
                this.isLoadingEntrees = false;
                this.refreshCount(patientId);
                this.syncView();
            },
            error: (err: unknown) => {
                this.errorMessage = this.extractErrorMessage(err, 'Erreur de chargement des souvenirs.');
                this.entrees = [];
                this.filteredEntrees = [];
                this.selectedEntree = null;
                this.isLoadingEntrees = false;
                this.totalEntrees = 0;
                this.syncView();
            }
        });
    }

    applyFilters(preferredSelectionId?: number | null): void {
        const query = this.searchTerm.trim().toLowerCase();
        const currentSelectionId = preferredSelectionId ?? this.selectedEntree?.id ?? null;

        this.filteredEntrees = this.entrees.filter((entree) => {
            const matchesQuery =
                !query
                || String(entree.id ?? '').includes(query)
                || String(entree.patientId).includes(query)
                || String(entree.doctorId).includes(query)
                || String(entree.caregiverId).includes(query)
                || (entree.mediaTitle ?? '').toLowerCase().includes(query)
                || entree.texte.toLowerCase().includes(query)
                || (entree.themeCulturel ?? '').toLowerCase().includes(query)
                || (entree.mediaType ?? '').toLowerCase().includes(query);

            return matchesQuery;
        });

        this.selectedEntree =
            this.filteredEntrees.find((entree) => entree.id === currentSelectionId)
            ?? this.filteredEntrees[0]
            ?? null;
    }

    selectEntree(entree: EntreeSouvenir): void {
        this.selectedEntree = entree;
    }

    openCreateForm(): void {
        this.showEntreeForm = true;
        this.isEditingEntree = false;
        this.editingEntreeId = null;
        this.entreeForm = this.initEntreeForm();
        this.errorMessage = '';
        this.successMessage = '';
    }

    editEntree(entree: EntreeSouvenir): void {
        this.showEntreeForm = true;
        this.isEditingEntree = true;
        this.editingEntreeId = entree.id ?? null;
        this.entreeForm = {
            id: entree.id,
            patientId: entree.patientId,
            doctorId: entree.doctorId,
            caregiverId: entree.caregiverId,
            infosCaregiver: entree.infosCaregiver ?? '',
            texte: entree.texte,
            mediaType: entree.mediaType,
            mediaUrl: entree.mediaUrl ?? '',
            mediaTitle: entree.mediaTitle ?? '',
            expectedSpeakerName: entree.expectedSpeakerName ?? '',
            expectedSpeakerRelation: entree.expectedSpeakerRelation ?? '',
            themeCulturel: entree.themeCulturel,
            traitee: entree.traitee
        };
        this.errorMessage = '';
        this.successMessage = '';
    }

    cancelEntreeForm(): void {
        this.showEntreeForm = false;
        this.isSavingEntree = false;
        this.isEditingEntree = false;
        this.editingEntreeId = null;
        this.entreeForm = this.initEntreeForm();
    }

    openPuzzleForm(entree: EntreeSouvenir): void {
        if (!this.canCreatePuzzle(entree)) {
            this.errorMessage = 'Seuls les souvenirs image peuvent etre transformes en puzzle.';
            return;
        }

        this.showPuzzleForm = true;
        this.puzzleSourceEntree = entree;
        this.puzzleForm = this.initPuzzleForm(entree);
        this.errorMessage = '';
        this.successMessage = '';
    }

    cancelPuzzleForm(): void {
        this.showPuzzleForm = false;
        this.isCreatingPuzzle = false;
        this.puzzleSourceEntree = null;
        this.puzzleForm = this.initPuzzleForm();
    }

    saveEntree(): void {
        if (!this.entreeForm.texte.trim()) {
            this.errorMessage = 'Le texte du souvenir est obligatoire.';
            return;
        }

        this.isSavingEntree = true;
        this.errorMessage = '';

        const payload: EntreeSouvenir = {
            patientId: this.entreeForm.patientId,
            doctorId: this.entreeForm.doctorId,
            caregiverId: this.entreeForm.caregiverId,
            infosCaregiver: this.entreeForm.infosCaregiver ?? '',
            texte: this.entreeForm.texte.trim(),
            mediaType: this.entreeForm.mediaType,
            mediaUrl: (this.entreeForm.mediaUrl ?? '').trim(),
            mediaTitle: this.entreeForm.mediaTitle ?? '',
            expectedSpeakerName: this.entreeForm.expectedSpeakerName ?? '',
            expectedSpeakerRelation: this.entreeForm.expectedSpeakerRelation ?? '',
            themeCulturel: this.entreeForm.themeCulturel
        };

        const onSuccess = (saved: EntreeSouvenir) => {
            this.successMessage = this.isEditingEntree
                ? 'Souvenir mis a jour avec succes.'
                : 'Souvenir cree avec succes.';
            this.showEntreeForm = false;
            this.isSavingEntree = false;
            this.patientFilterInput = String(saved.patientId);
            this.selectedEntree = saved;
            this.syncView();
            this.loadByPatient(saved.id ?? null);
        };

        if (this.isEditingEntree && this.editingEntreeId) {
            this.souvenirsService.updateEntree(this.editingEntreeId, payload).subscribe({
                next: onSuccess,
                error: (err: unknown) => {
                    this.errorMessage = this.extractErrorMessage(err, 'Erreur lors de la mise a jour du souvenir.');
                    this.isSavingEntree = false;
                    this.syncView();
                }
            });
            return;
        }

        this.souvenirsService.createEntree(payload).subscribe({
            next: onSuccess,
            error: (err: unknown) => {
                this.errorMessage = this.extractErrorMessage(err, 'Erreur lors de la creation du souvenir.');
                this.isSavingEntree = false;
                this.syncView();
            }
        });
    }

    createPuzzle(): void {
        if (!this.canCreatePuzzle(this.puzzleSourceEntree)) {
            this.errorMessage = 'Selectionne un souvenir image valide avant de creer un puzzle.';
            return;
        }

        this.isCreatingPuzzle = true;
        this.errorMessage = '';

        const payload: PuzzleCreateRequest = {
            souvenirEntryId: this.puzzleSourceEntree.id,
            patientId: this.puzzleSourceEntree.patientId,
            title: this.puzzleForm.title,
            description: this.puzzleForm.description,
            difficulty: this.puzzleForm.difficulty,
            timeLimitSeconds: this.puzzleForm.timeLimitSeconds,
            maxHints: this.puzzleForm.maxHints
        };

        this.recommendationService.createPuzzle(payload).subscribe({
            next: (created) => {
                this.successMessage =
                    `Puzzle "${created.title}" cree avec succes (event #${created.medicalEventId}).`;
                this.cancelPuzzleForm();
                this.syncView();
            },
            error: (err: unknown) => {
                this.errorMessage = this.extractErrorMessage(err, 'Erreur lors de la creation du puzzle.');
                this.isCreatingPuzzle = false;
                this.syncView();
            }
        });
    }

    deleteEntree(entree: EntreeSouvenir): void {
        if (!entree.id || !confirm('Supprimer ce souvenir ?')) {
            return;
        }

        this.souvenirsService.deleteEntree(entree.id).subscribe({
            next: () => {
                this.successMessage = 'Souvenir supprime.';
                this.loadByPatient();
                this.syncView();
            },
            error: (err: unknown) => {
                this.errorMessage = this.extractErrorMessage(err, 'Erreur lors de la suppression du souvenir.');
                this.syncView();
            }
        });
    }

    markTraitee(entree: EntreeSouvenir): void {
        if (!entree.id) {
            return;
        }

        this.souvenirsService.markTraitee(entree.id).subscribe({
            next: () => {
                this.successMessage = 'Souvenir marque comme traite.';
                this.loadByPatient(entree.id);
                this.syncView();
            },
            error: (err: unknown) => {
                this.errorMessage = this.extractErrorMessage(err, 'Erreur lors du marquage.');
                this.syncView();
            }
        });
    }

    getMediaBadgeClass(mediaType: MediaType): string {
        return mediaType === MediaType.IMAGE ? 'media-chip chip-image' : 'media-chip chip-audio';
    }

    getProcessingBadgeClass(entree: EntreeSouvenir): string {
        return entree.traitee ? 'status-badge badge-treated' : 'status-badge badge-pending';
    }

    getProcessingLabel(entree: EntreeSouvenir): string {
        return entree.traitee ? 'TRAITEE' : 'EN_ATTENTE';
    }

    trackEntree(index: number, entree: EntreeSouvenir): number {
        return entree.id ?? index;
    }

    formatDateTime(value?: string | null): string {
        if (!value) {
            return '-';
        }

        const date = new Date(value);
        return Number.isNaN(date.getTime()) ? value : date.toLocaleString();
    }

    private refreshCount(patientId: number): void {
        this.souvenirsService.countEntreesByPatient(patientId).subscribe({
            next: (data) => {
                this.totalEntrees = data.totalEntrees;
                this.syncView();
            },
            error: () => {
                this.totalEntrees = this.entrees.length;
                this.syncView();
            }
        });
    }

    private initEntreeForm(): EntreeSouvenir {
        return {
            patientId: 4,
            doctorId: 3,
            caregiverId: 10,
            infosCaregiver: '',
            texte: '',
            mediaType: MediaType.IMAGE,
            mediaUrl: '',
            mediaTitle: '',
            expectedSpeakerName: '',
            expectedSpeakerRelation: '',
            themeCulturel: ThemeCulturel.TRADITIONS
        };
    }

    private initPuzzleForm(entree?: EntreeSouvenir): PuzzleFormState {
        return {
            title: entree?.mediaTitle ? `Puzzle - ${entree.mediaTitle}` : '',
            description: entree?.texte ?? '',
            difficulty: DifficultyLevel.EASY,
            timeLimitSeconds: 300,
            maxHints: 3
        };
    }

    private canCreatePuzzle(entree: EntreeSouvenir | null): entree is EntreeSouvenir & { id: number } {
        return !!entree?.id && entree.mediaType === MediaType.IMAGE;
    }

    private extractErrorMessage(err: unknown, fallback: string): string {
        if (err instanceof HttpErrorResponse) {
            if (typeof err.error === 'string') {
                return err.error;
            }
            if (err.error?.message) {
                return err.error.message;
            }
            if (err.message) {
                return err.message;
            }
        }
        return fallback;
    }

    private syncView(): void {
        this.cdr.detectChanges();
    }
}
