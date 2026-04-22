import { Component, OnInit, OnDestroy, NgZone, ChangeDetectorRef } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Subscription } from 'rxjs';
import { MlRecommendationService, RecommendationResult } from '../../services/ml-recommendation.service';

@Component({
  selector: 'app-ml-recommendation',
  templateUrl: './ml-recommendation.html',
  styleUrls: ['./ml-recommendation.css'],
  standalone: false,
})
export class MlRecommendationComponent implements OnInit, OnDestroy {
  form!: FormGroup;
  result: RecommendationResult | null = null;
  loading = false;
  error = '';
  submitted = false;
  private sub?: Subscription;

  // For grouping recommendations by category in the template
  get categories(): string[] {
    if (!this.result) return [];
    return [...new Set(this.result.recommendations.map(r => r.category))];
  }

  recsByCategory(cat: string) {
    return this.result?.recommendations.filter(r => r.category === cat) ?? [];
  }

  get clusterColor(): string {
    const label = this.result?.cluster_label ?? '';
    if (label.includes('Low'))      return '#4CAF50';
    if (label.includes('Medium') || label.includes('Moderate')) return '#FF9800';
    if (label.includes('High'))     return '#F44336';
    if (label.includes('Critical')) return '#9C27B0';
    return '#2196F3';
  }

  get priorityHighCount() { return this.result?.recommendations.filter(r => r.priority === 'High').length ?? 0; }
  get priorityMedCount()  { return this.result?.recommendations.filter(r => r.priority === 'Medium').length ?? 0; }
  get maxContribution(): number {
    if (!this.result?.recommendations?.length) return 1;
    return Math.max(...this.result.recommendations.map(r => r.contribution_score)) || 1;
  }

  categoryIcon(cat: string): string {
    const map: Record<string, string> = {
      'Lifestyle':      'bi-heart-pulse-fill',
      'Clinical':       'bi-droplet-fill',
      'Cognitive':      'bi-brain',
      'Mental Health':  'bi-emoji-neutral-fill',
      'Preventive':     'bi-shield-plus-fill',
    };
    return map[cat] ?? 'bi-star-fill';
  }

  constructor(
    private fb: FormBuilder,
    private mlService: MlRecommendationService,
    private zone: NgZone,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      // Demographics
      Age:            [70, [Validators.required, Validators.min(60), Validators.max(90)]],
      Gender:         [1],
      Ethnicity:      [0],
      EducationLevel: [2],
      // Lifestyle
      BMI:                [24.0, Validators.required],
      Smoking:            [0],
      AlcoholConsumption: [4,   Validators.required],
      PhysicalActivity:   [4.0, Validators.required],
      DietQuality:        [7.0, Validators.required],
      SleepQuality:       [8.0, Validators.required],
      // Medical history
      FamilyHistoryAlzheimers: [0],
      CardiovascularDisease:   [0],
      Diabetes:                [0],
      Depression:              [0],
      HeadInjury:              [0],
      Hypertension:            [0],
      // Clinical
      SystolicBP:              [118, Validators.required],
      DiastolicBP:             [76,  Validators.required],
      CholesterolTotal:        [185, Validators.required],
      CholesterolLDL:          [95,  Validators.required],
      CholesterolHDL:          [65,  Validators.required],
      CholesterolTriglycerides:[140, Validators.required],
      // Cognitive
      MMSE:                [28, Validators.required],
      FunctionalAssessment:[8,  Validators.required],
      MemoryComplaints:    [0],
      BehavioralProblems:  [0],
      ADL:                 [9,  Validators.required],
      // Symptoms (binary — 0 is valid)
      Confusion:                [0],
      Disorientation:           [0],
      PersonalityChanges:       [0],
      DifficultyCompletingTasks:[0],
      Forgetfulness:            [0],
    });
  }

  submit(): void {
    console.log('[ML] submit() called');
    this.submitted = true;
    console.log('[ML] form valid:', this.form.valid, 'errors:', this.form.errors);

    if (this.form.invalid) {
      console.log('[ML] form is INVALID — aborting', this.form.value);
      return;
    }

    this.loading = true;
    this.error   = '';
    this.result  = null;

    // Coerce all values to numbers
    const raw = this.form.value;
    const payload: any = {};
    for (const key of Object.keys(raw)) {
      payload[key] = Number(raw[key]);
    }
    console.log('[ML] sending payload:', payload);

    fetch('http://localhost:5000/recommend', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    })
      .then(res => {
        console.log('[ML] fetch status:', res.status);
        return res.json();
      })
      .then(data => {
        console.log('[ML] response data:', data);
        this.zone.run(() => {
          this.result  = data;
          this.loading = false;
          this.cdr.detectChanges();
          console.log('[ML] view updated — recommendations:', data.total_recommendations);
        });
      })
      .catch(err => {
        console.error('[ML] fetch error:', err);
        this.zone.run(() => {
          this.error   = `Could not reach http://localhost:5000. Make sure python app.py is running.`;
          this.loading = false;
          this.cdr.detectChanges();
        });
      });
  }

  reset(): void {
    this.sub?.unsubscribe();
    this.result    = null;
    this.submitted = false;
    this.error     = '';
    this.loading   = false;
    this.form.reset();
    this.ngOnInit();
  }

  ngOnDestroy(): void { this.sub?.unsubscribe(); }
}
