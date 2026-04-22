import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { FollowUp, MoodState, IndependenceLevel, SleepQuality, FollowUpStats } from './followup.model';
import { FollowUpService } from './followup.service';

@Component({
  selector: 'app-followup-page',
  standalone: false,
  templateUrl: './followup-page.html',
  styleUrls: ['./followup-page.css'],
})
export class FollowUpPage implements OnInit {
  followUps: FollowUp[] = [];
  filteredFollowUps: FollowUp[] = [];

  isViewOpen = false;
  viewFollowUp: FollowUp | null = null;

  isFormOpen = false;
  formMode: 'create' | 'edit' = 'create';
  formStep = 0;
  formSteps = ['Patient Info', 'Cognitive & Mood', 'ADL & Sleep', 'Vital Signs & Notes'];
  editingId: number | null = null;
  submitAttempted = false;
  formError = '';
  pageError = '';
  successMsg = '';
  isSaving = false;
  form: FormGroup;

  showStats = false;
  statistics: FollowUpStats | null = null;
  riskPatientId: number | null = null;
  showRiskPanel = false;
  riskData: any = null;

  filters = {
    query: '',
    mood: '' as '' | MoodState,
    sleepQuality: '' as '' | SleepQuality,
  };

  readonly moodOptions = [
    { label: '😌 Calm', value: 'CALM' as MoodState },
    { label: '😊 Happy', value: 'HAPPY' as MoodState },
    { label: '😰 Anxious', value: 'ANXIOUS' as MoodState },
    { label: '😡 Agitated', value: 'AGITATED' as MoodState },
    { label: '😞 Depressed', value: 'DEPRESSED' as MoodState },
    { label: '😵 Confused', value: 'CONFUSED' as MoodState },
  ];

  readonly sleepQualityOptions = [
    { label: 'Excellent', value: 'EXCELLENT' as SleepQuality },
    { label: 'Good', value: 'GOOD' as SleepQuality },
    { label: 'Fair', value: 'FAIR' as SleepQuality },
    { label: 'Poor', value: 'POOR' as SleepQuality },
  ];

  readonly independenceLevels = [
    { label: 'Independent', value: 'INDEPENDENT' as IndependenceLevel },
    { label: 'Needs Assistance', value: 'NEEDS_ASSISTANCE' as IndependenceLevel },
    { label: 'Dependent', value: 'DEPENDENT' as IndependenceLevel },
  ];

  constructor(private fb: FormBuilder, private followUpService: FollowUpService) {
    this.form = this.fb.group({
      patientId: [null, [Validators.required, Validators.min(1)]],
      caregiverId: [null, [Validators.required, Validators.min(1)]],
      followUpDate: ['', Validators.required],
      cognitiveScore: [null],
      mood: [null],
      agitationObserved: [false],
      confusionObserved: [false],
      eating: [null],
      dressing: [null],
      mobility: [null],
      hoursSlept: [null],
      sleepQuality: [null],
      notes: [''],
      vitalSigns: [''],
    });
  }

  ngOnInit(): void {
    this.loadFollowUps();
  }

  private loadFollowUps(): void {
    this.followUpService.getAll().subscribe({
      next: (data) => {
        this.followUps = data;
        this.applyFilters();
      },
      error: () => {
        this.pageError = 'Unable to load follow-ups. Check if followup-alert-service is running on port 8090.';
      },
    });
  }

  applyFilters(): void {
    const q = this.filters.query.trim().toLowerCase();
    this.filteredFollowUps = this.followUps.filter((fu) => {
      const matchQuery =
        !q ||
        String(fu.patientId ?? '').includes(q) ||
        String(fu.caregiverId ?? '').includes(q) ||
        (fu.mood ?? '').toLowerCase().includes(q) ||
        (fu.notes ?? '').toLowerCase().includes(q);
      const matchMood = !this.filters.mood || fu.mood === this.filters.mood;
      const matchSleep = !this.filters.sleepQuality || fu.sleepQuality === this.filters.sleepQuality;
      return matchQuery && matchMood && matchSleep;
    });
  }

  getCalmCount(): number {
    return this.followUps.filter((f) => f.mood === 'CALM' || f.mood === 'HAPPY').length;
  }

  getAgitatedCount(): number {
    return this.followUps.filter((f) => f.mood === 'AGITATED' || f.mood === 'ANXIOUS').length;
  }

  getAvgCognitive(): string {
    const scores = this.followUps.filter((f) => f.cognitiveScore != null).map((f) => f.cognitiveScore!);
    return scores.length === 0 ? '-' : (scores.reduce((a, b) => a + b, 0) / scores.length).toFixed(1);
  }

  getMoodIcon(mood: MoodState | null | undefined): string {
    const icons: Record<string, string> = {
      CALM: '😌', HAPPY: '😊', ANXIOUS: '😰', AGITATED: '😡', DEPRESSED: '😞', CONFUSED: '😵',
    };
    return icons[mood ?? ''] ?? '';
  }

  getCognitiveClass(score: number | null): string {
    if (score == null) return '';
    if (score >= 24) return 'cog-high';
    if (score >= 18) return 'cog-medium';
    return 'cog-low';
  }

  getCognitivePercent(score: number | null): number {
    if (score == null) return 0;
    return Math.min(100, (score / 30) * 100);
  }

  openViewModal(fu: FollowUp): void {
    this.viewFollowUp = fu;
    this.isViewOpen = true;
  }

  closeViewModal(): void {
    this.isViewOpen = false;
    this.viewFollowUp = null;
  }

  openCreateModal(): void {
    this.formMode = 'create';
    this.editingId = null;
    this.formStep = 0;
    this.submitAttempted = false;
    this.formError = '';
    this.form.reset();
    this.isFormOpen = true;
  }

  openEditModal(fu: FollowUp): void {
    this.formMode = 'edit';
    this.editingId = fu.id ?? null;
    this.formStep = 0;
    this.submitAttempted = false;
    this.formError = '';
    this.form.patchValue(fu);
    this.isFormOpen = true;
  }

  closeFormModal(): void {
    this.isFormOpen = false;
    this.formError = '';
    this.isSaving = false;
    this.formStep = 0;
  }

  nextStep(): void {
    if (this.formStep === 0) {
      this.submitAttempted = true;
      const p = this.form.get('patientId');
      const c = this.form.get('caregiverId');
      const d = this.form.get('followUpDate');
      if (p?.invalid || c?.invalid || d?.invalid) return;
    }
    this.submitAttempted = false;
    if (this.formStep < this.formSteps.length - 1) this.formStep++;
  }

  prevStep(): void {
    if (this.formStep > 0) this.formStep--;
  }

  goToStep(step: number): void {
    if (step <= this.formStep) this.formStep = step;
  }

  validateAndSave(): void {
    this.formError = '';
    this.submitAttempted = true;
    this.form.markAllAsTouched();

    const required = ['patientId', 'caregiverId', 'followUpDate'];
    for (const key of required) {
      if (this.form.get(key)?.invalid) {
        this.formStep = 0;
        return;
      }
    }

    const v = this.form.value;
    const payload: Partial<FollowUp> = {
      patientId: Number(v.patientId),
      caregiverId: Number(v.caregiverId),
      followUpDate: v.followUpDate || undefined,
      cognitiveScore: v.cognitiveScore != null ? Number(v.cognitiveScore) : undefined,
      mood: v.mood || undefined,
      agitationObserved: !!v.agitationObserved,
      confusionObserved: !!v.confusionObserved,
      eating: v.eating || undefined,
      dressing: v.dressing || undefined,
      mobility: v.mobility || undefined,
      hoursSlept: v.hoursSlept != null ? Number(v.hoursSlept) : undefined,
      sleepQuality: v.sleepQuality || undefined,
      notes: v.notes || undefined,
      vitalSigns: v.vitalSigns || undefined,
    };

    this.isSaving = true;

    if (this.formMode === 'create') {
      this.followUpService.create(payload).subscribe({
        next: () => {
          this.closeFormModal();
          this.loadFollowUps();
          this.successMsg = 'Follow-up created successfully!';
          setTimeout(() => (this.successMsg = ''), 3000);
        },
        error: (err) => {
          this.isSaving = false;
          this.formError = err?.error?.message ?? 'Failed to create follow-up.';
        },
      });
    } else {
      if (!this.editingId) {
        this.isSaving = false;
        return;
      }
      this.followUpService.update(this.editingId, payload).subscribe({
        next: () => {
          this.closeFormModal();
          this.loadFollowUps();
          this.successMsg = 'Follow-up updated successfully!';
          setTimeout(() => (this.successMsg = ''), 3000);
        },
        error: (err) => {
          this.isSaving = false;
          this.formError = err?.error?.message ?? 'Failed to update follow-up.';
        },
      });
    }
  }

  deleteFollowUp(fu: FollowUp): void {
    if (!fu.id) return;
    if (!confirm(`Delete follow-up #${fu.id} for Patient #${fu.patientId}?`)) return;
    this.followUpService.delete(fu.id).subscribe({
      next: () => {
        this.loadFollowUps();
        this.successMsg = 'Follow-up deleted successfully!';
        setTimeout(() => (this.successMsg = ''), 3000);
      },
      error: () => {
        this.pageError = 'Failed to delete follow-up.';
      },
    });
  }

  toggleStats(): void {
    this.showStats = !this.showStats;
    if (this.showStats && !this.statistics) {
      this.followUpService.getStatistics().subscribe({
        next: (data) => (this.statistics = data),
        error: () => (this.pageError = 'Failed to load statistics.'),
      });
    }
  }

  loadPatientRisk(): void {
    if (!this.riskPatientId || this.riskPatientId < 1) return;
    this.followUpService.getPatientRisk(this.riskPatientId).subscribe({
      next: (data) => {
        this.riskData = data;
        this.showRiskPanel = true;
      },
      error: () => (this.pageError = 'Failed to calculate risk. Ensure patient has follow-ups.'),
    });
  }

  checkCognitiveDecline(): void {
    if (!this.riskPatientId || this.riskPatientId < 1) return;
    this.followUpService.detectCognitiveDecline(this.riskPatientId).subscribe({
      next: (data) => {
        this.successMsg = data.cognitiveDecline
          ? `⚠️ Cognitive decline detected for Patient #${this.riskPatientId}!`
          : `✅ No cognitive decline for Patient #${this.riskPatientId}.`;
        setTimeout(() => (this.successMsg = ''), 5000);
      },
      error: () => (this.pageError = 'Failed to check cognitive decline.'),
    });
  }

  getRiskColor(): string {
    if (!this.riskData) return '#9ca3b4';
    const level = this.riskData.riskLevel;
    if (level === 'CRITICAL') return '#6f42c1';
    if (level === 'HIGH') return '#e24653';
    if (level === 'MODERATE') return '#cc8a13';
    return '#049f89';
  }

  controlHasError(name: string): boolean {
    const ctrl = this.form.get(name);
    return !!ctrl && this.submitAttempted && ctrl.invalid;
  }

  trackById(_: number, fu: FollowUp): number {
    return fu.id ?? _;
  }
}
