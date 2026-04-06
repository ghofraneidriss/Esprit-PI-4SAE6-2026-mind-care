import { Component, OnInit } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import {
    EntreeSouvenir,
    MediaType,
    ThemeCulturel
} from './souvenirs.model';
import { SouvenirsService } from './souvenirs.service';

@Component({
    selector: 'app-souvenirs',
    templateUrl: './souvenirs.html',
    styleUrls: ['./souvenirs.css'],
    standalone: false
})
export class SouvenirsPage implements OnInit {
    entrees: EntreeSouvenir[] = [];

    isLoadingEntrees = false;
    isSavingEntree = false;

    showEntreeForm = false;
    isEditingEntree = false;
    editingEntreeId: number | null = null;
    selectedEntree: EntreeSouvenir | null = null;

    patientFilterInput = '';
    selectedPatientId: number | null = null;
    themeFilter: ThemeCulturel | '' = '';
    mediaTypeFilter: MediaType | '' = '';

    totalEntrees = 0;

    errorMessage = '';
    successMessage = '';

    themeOptions = Object.values(ThemeCulturel);
    mediaTypeOptions = Object.values(MediaType);

    entreeForm: EntreeSouvenir = this.initEntreeForm();

    constructor(private souvenirsService: SouvenirsService) { }

    ngOnInit(): void {
        this.patientFilterInput = '4';
        this.loadByPatient();
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
            this.themeFilter = '';
            this.mediaTypeFilter = '';
            this.selectedEntree = saved;
            this.loadByPatient();
        };

        if (this.isEditingEntree && this.editingEntreeId) {
            this.souvenirsService.updateEntree(this.editingEntreeId, payload).subscribe({
                next: onSuccess,
                error: (err: unknown) => {
                    this.errorMessage = this.extractErrorMessage(err, 'Erreur lors de la mise a jour du souvenir.');
                    this.isSavingEntree = false;
                }
            });
            return;
        }

        this.souvenirsService.createEntree(payload).subscribe({
            next: onSuccess,
            error: (err: unknown) => {
                this.errorMessage = this.extractErrorMessage(err, 'Erreur lors de la creation du souvenir.');
                this.isSavingEntree = false;
            }
        });
    }

    loadByPatient(): void {
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
                this.isLoadingEntrees = false;
                this.totalEntrees = data.length;
                if (this.selectedEntree?.id) {
                    this.selectedEntree = data.find((e) => e.id === this.selectedEntree?.id) ?? null;
                }
                this.refreshCount(patientId);
            },
            error: (err: unknown) => {
                this.errorMessage = this.extractErrorMessage(err, 'Erreur de chargement des souvenirs.');
                this.entrees = [];
                this.isLoadingEntrees = false;
                this.totalEntrees = 0;
                this.selectedEntree = null;
            }
        });
    }

    deleteEntree(entree: EntreeSouvenir): void {
        if (!entree.id) return;
        if (!confirm('Supprimer ce souvenir ?')) return;

        this.souvenirsService.deleteEntree(entree.id).subscribe({
            next: () => {
                this.successMessage = 'Souvenir supprime.';
                if (this.selectedEntree?.id === entree.id) {
                    this.selectedEntree = null;
                }
                this.loadByPatient();
            },
            error: (err: unknown) => {
                this.errorMessage = this.extractErrorMessage(err, 'Erreur lors de la suppression du souvenir.');
            }
        });
    }

    markTraitee(entree: EntreeSouvenir): void {
        if (!entree.id) return;
        this.souvenirsService.markTraitee(entree.id).subscribe({
            next: () => {
                this.successMessage = 'Souvenir marque comme traite.';
                this.loadByPatient();
            },
            error: (err: unknown) => {
                this.errorMessage = this.extractErrorMessage(err, 'Erreur lors du marquage.');
            }
        });
    }

    selectEntree(entree: EntreeSouvenir): void {
        this.selectedEntree = entree;
    }

    private refreshCount(patientId: number): void {
        this.souvenirsService.countEntreesByPatient(patientId).subscribe({
            next: (data) => {
                this.totalEntrees = data.totalEntrees;
            },
            error: () => {
                this.totalEntrees = this.entrees.length;
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

    private extractErrorMessage(err: unknown, fallback: string): string {
        if (err instanceof HttpErrorResponse) {
            if (typeof err.error === 'string') return err.error;
            if (err.error?.message) return err.error.message;
            if (err.message) return err.message;
        }
        return fallback;
    }
}
