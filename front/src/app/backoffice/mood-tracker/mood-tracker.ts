import { Component, OnInit, OnDestroy, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MoodService, MoodEntry } from '../../core/services/mood.service';
import { Chart, registerables } from 'chart.js';

Chart.register(...registerables);

@Component({
  selector: 'app-mood-tracker',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './mood-tracker.html',
  styleUrl: './mood-tracker.css'
})
export class MoodTrackerComponent implements OnInit, OnDestroy {
  @ViewChild('moodChart') moodChartRef!: ElementRef<HTMLCanvasElement>;

  entries: MoodEntry[] = [];
  loading = false;
  saving = false;
  chart: Chart | null = null;

  patientId = 1;

  form: Partial<MoodEntry> = {
    date: new Date().toISOString().split('T')[0],
    score: 3,
    note: ''
  };

  moodLabels: Record<number, string> = {
    1: '😢 Very Bad',
    2: '😕 Bad',
    3: '😐 Neutral',
    4: '🙂 Good',
    5: '😊 Excellent'
  };

  moodColors: Record<number, string> = {
    1: '#dc3545',
    2: '#fd7e14',
    3: '#6c757d',
    4: '#0dcaf0',
    5: '#198754'
  };

  constructor(private moodService: MoodService) {}

  ngOnInit(): void {
    this.load();
  }

  ngOnDestroy(): void {
    this.chart?.destroy();
  }

  load(): void {
    this.loading = true;
    this.moodService.getByPatient(this.patientId).subscribe({
      next: data => {
        this.entries = data;
        this.loading = false;
        setTimeout(() => this.renderChart(), 100);
      },
      error: () => { this.loading = false; }
    });
  }

  renderChart(): void {
    if (!this.moodChartRef) return;
    this.chart?.destroy();

    const sorted = [...this.entries]
      .sort((a, b) => a.date.localeCompare(b.date))
      .slice(-14);

    this.chart = new Chart(this.moodChartRef.nativeElement, {
      type: 'line',
      data: {
        labels: sorted.map(e => e.date),
        datasets: [{
          label: 'Mood Score',
          data: sorted.map(e => e.score),
          fill: true,
          backgroundColor: 'rgba(13, 110, 253, 0.1)',
          borderColor: '#0d6efd',
          tension: 0.4,
          pointBackgroundColor: sorted.map(e => this.moodColors[e.score]),
          pointRadius: 7
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        scales: {
          y: { min: 1, max: 5, ticks: { stepSize: 1 } }
        },
        plugins: { legend: { display: false } }
      }
    });
  }

  save(): void {
    this.saving = true;
    const entry: MoodEntry = {
      patientId: this.patientId,
      date: this.form.date!,
      score: this.form.score!,
      note: this.form.note
    };
    this.moodService.create(entry).subscribe({
      next: () => {
        this.saving = false;
        this.form = {
          date: new Date().toISOString().split('T')[0],
          score: 3,
          note: ''
        };
        this.load();
      },
      error: () => { this.saving = false; }
    });
  }

  delete(id: number): void {
    this.moodService.delete(id).subscribe(() => this.load());
  }

  getScoreLabel(score: number): string {
    return this.moodLabels[score] || '';
  }

  getScoreColor(score: number): string {
    const map: Record<number, string> = {
      1: 'danger', 2: 'warning', 3: 'secondary', 4: 'info', 5: 'success'
    };
    return map[score] || 'secondary';
  }

  getAvgScore(): string {
    if (!this.entries.length) return '—';
    const avg = this.entries.reduce((s, e) => s + e.score, 0) / this.entries.length;
    return avg.toFixed(1);
  }
}
