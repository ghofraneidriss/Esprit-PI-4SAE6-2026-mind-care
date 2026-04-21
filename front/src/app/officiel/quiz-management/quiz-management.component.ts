import { Component, OnInit, ChangeDetectorRef, Input, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { QuizPreviewApi, QuizService } from '../../core/services/quiz.service';
import { ToastService } from '../../core/services/toast.service';
import { ConfirmDialogService } from '../../core/services/confirm-dialog.service';
import {
  ADMIN_TABLE_PAGE_SIZE,
  padPageRows,
  slicePage,
  totalPageCount
} from '../../core/utils/admin-table-paging';

/** Quiz loaded for read-only preview (same shape as the player, no interaction). */
export interface PreviewQuiz {
  title: string;
  description?: string;
  difficulty?: string;
  questions: { questionText: string; options: string[] }[];
}

@Component({
  selector: 'app-off-quiz-mgmt',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './quiz-management.component.html',
  styleUrls: ['./quiz-management.component.css', '../mgmt-shared.css']
})
export class QuizManagementComponent implements OnInit {
  /** Rendered inside dashboard backoffice: lighter margins and header. */
  @Input() embedded = false;

  quizzes: any[] = [];
  loading = true;
  showForm = false;
  editing = false;

  filterSearch = '';
  filterDifficulty: '' | 'EASY' | 'MEDIUM' | 'HARD' = '';
  filterTheme: '' | 'MEMORY' | 'LOGIC' | 'LANGUAGE' | 'ATTENTION' | 'GENERAL' = '';

  pageIndex = 0;
  readonly pageSize = ADMIN_TABLE_PAGE_SIZE;

  selectedIds: number[] = [];

  form: any = this.emptyForm();

  /** Avoid repeat HTTP when reopening the same quiz in the session. */
  private readonly previewCache = new Map<number, PreviewQuiz>();

  constructor(
    private quizSvc: QuizService,
    private cdr: ChangeDetectorRef,
    private toast: ToastService,
    private confirm: ConfirmDialogService
  ) {}

  ngOnInit() {
    this.load();
  }

  emptyForm() {
    return {
      id: null,
      title: '',
      description: '',
      difficulty: 'EASY',
      theme: 'MEMORY',
      type: 'QUIZ',
      questions: [{ questionText: '', options: ['', '', '', ''], correctAnswer: 0 }]
    };
  }

  /** Transform backend question → UI form question */
  private backendToForm(q: any): any {
    const opts = [q.optionA || '', q.optionB || '', q.optionC || '', q.optionD || ''];
    const answerMap: any = { A: 0, B: 1, C: 2, D: 3 };
    const ca = answerMap[(q.correctAnswer || 'A').toUpperCase()] ?? 0;
    return { questionText: q.text || '', options: opts, correctAnswer: ca, score: q.score || 10 };
  }

  /** Transform UI form question → backend question */
  private formToBackend(q: any): any {
    const letters = ['A', 'B', 'C', 'D'];
    return {
      text: q.questionText || '',
      optionA: q.options?.[0] || '',
      optionB: q.options?.[1] || '',
      optionC: q.options?.[2] || '',
      optionD: q.options?.[3] || '',
      correctAnswer: letters[q.correctAnswer] || 'A',
      score: q.score || 10
    };
  }

  get filteredQuizzes(): any[] {
    let list = [...this.quizzes];
    const q = (this.filterSearch || '').trim().toLowerCase();
    if (q) {
      list = list.filter(
        (x) =>
          (x.title || '').toLowerCase().includes(q) ||
          (x.description || '').toLowerCase().includes(q)
      );
    }
    if (this.filterDifficulty) {
      list = list.filter((x) => String(x.difficulty || '').toUpperCase() === this.filterDifficulty);
    }
    if (this.filterTheme) {
      list = list.filter((x) => String(x.theme || '').toUpperCase() === this.filterTheme);
    }
    return list;
  }

  get pagedFilteredQuizzes(): any[] {
    return slicePage(this.filteredQuizzes, this.pageIndex, this.pageSize);
  }

  get quizTableRows(): (any | null)[] {
    if (this.loading || !this.filteredQuizzes.length) return [];
    return padPageRows(this.pagedFilteredQuizzes, this.pageSize);
  }

  get quizTotalPages(): number {
    return totalPageCount(this.filteredQuizzes.length, this.pageSize);
  }

  get quizRangeLabel(): string {
    const n = this.filteredQuizzes.length;
    if (!n) return '';
    const start = this.pageIndex * this.pageSize + 1;
    const end = Math.min(n, (this.pageIndex + 1) * this.pageSize);
    return `${start}–${end} of ${n}`;
  }

  quizFiltersChanged() {
    this.pageIndex = 0;
  }

  quizPrevPage() {
    if (this.pageIndex > 0) this.pageIndex--;
  }

  quizNextPage() {
    if (this.pageIndex < this.quizTotalPages - 1) this.pageIndex++;
  }

  get allFilteredSelected(): boolean {
    const f = this.pagedFilteredQuizzes;
    return f.length > 0 && f.every((x) => this.selectedIds.includes(x.id));
  }

  isSelected(id: number): boolean {
    return this.selectedIds.includes(id);
  }

  toggleRow(id: number, checked: boolean) {
    if (checked) {
      if (!this.selectedIds.includes(id)) this.selectedIds = [...this.selectedIds, id];
    } else {
      this.selectedIds = this.selectedIds.filter((x) => x !== id);
    }
  }

  toggleSelectAll(checked: boolean) {
    const ids = this.pagedFilteredQuizzes.map((q) => q.id);
    if (checked) {
      const set = new Set([...this.selectedIds, ...ids]);
      this.selectedIds = [...set];
    } else {
      const idSet = new Set(ids);
      this.selectedIds = this.selectedIds.filter((id) => !idSet.has(id));
    }
  }

  resetFilters() {
    this.filterSearch = '';
    this.filterDifficulty = '';
    this.filterTheme = '';
    this.selectedIds = [];
    this.pageIndex = 0;
  }

  load() {
    this.loading = true;
    this.previewCache.clear();
    this.quizSvc.getQuizzes().subscribe({
      next: (d: any) => {
        this.quizzes = d || [];
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (e) => {
        this.loading = false;
        this.toast.showHttpError(e, 'Could not load quizzes. Please refresh the page.');
        this.cdr.detectChanges();
      }
    });
  }

  openCreate() {
    this.form = this.emptyForm();
    this.editing = false;
    this.showForm = true;
  }

  openEdit(q: any) {
    const clone = JSON.parse(JSON.stringify(q));
    this.form = {
      id: clone.id,
      title: clone.title || '',
      description: clone.description || '',
      difficulty: clone.difficulty || 'EASY',
      theme: clone.theme || 'MEMORY',
      type: clone.type || 'QUIZ',
      questions: (clone.questions || []).map((qq: any) => this.backendToForm(qq))
    };
    if (!this.form.questions.length) {
      this.form.questions = [{ questionText: '', options: ['', '', '', ''], correctAnswer: 0 }];
    }
    this.editing = true;
    this.showForm = true;
  }

  addQuestion() {
    this.form.questions.push({ questionText: '', options: ['', '', '', ''], correctAnswer: 0 });
  }

  removeQuestion(i: number) {
    if (this.form.questions.length > 1) this.form.questions.splice(i, 1);
  }

  save() {
    const payload: any = {
      title: this.form.title,
      description: this.form.description,
      difficulty: this.form.difficulty,
      level: this.form.difficulty,
      theme: this.form.theme || 'MEMORY',
      type: this.form.type || 'QUIZ',
      status: 'ACTIVE',
      questions: (this.form.questions || []).map((q: any) => this.formToBackend(q))
    };
    if (this.editing) {
      this.quizSvc.updateQuiz(this.form.id, payload).subscribe({
        next: () => {
          this.toast.show('Quiz updated', 'success');
          this.showForm = false;
          this.load();
          this.cdr.detectChanges();
        },
        error: (e) => {
          this.toast.showHttpError(e, 'Could not update the quiz. Please try again.');
          this.cdr.detectChanges();
        }
      });
    } else {
      this.quizSvc.createQuiz(payload).subscribe({
        next: () => {
          this.toast.show('Quiz created', 'success');
          this.showForm = false;
          this.load();
          this.cdr.detectChanges();
        },
        error: (e) => {
          this.toast.showHttpError(e, 'Could not create the quiz. Please try again.');
          this.cdr.detectChanges();
        }
      });
    }
  }

  async deleteOne(id: number) {
    const ok = await this.confirm.confirm({
      title: 'Delete quiz',
      message: 'Are you sure you want to delete this quiz? This cannot be undone.',
      confirmText: 'Delete',
      cancelText: 'Cancel',
      danger: true
    });
    if (!ok) return;
    this.quizSvc.deleteQuiz(id).subscribe({
      next: () => {
        this.toast.show('Quiz deleted', 'success');
        this.selectedIds = this.selectedIds.filter((x) => x !== id);
        this.load();
        this.cdr.detectChanges();
      },
      error: (e) => {
        this.toast.showHttpError(e, 'Could not delete this quiz. Please try again.');
        this.cdr.detectChanges();
      }
    });
  }

  async deleteSelected() {
    const ids = [...this.selectedIds];
    if (!ids.length) return;
    const ok = await this.confirm.confirm({
      title: 'Delete quizzes',
      message: `Delete ${ids.length} quiz(zes)? This cannot be undone.`,
      confirmText: 'Delete',
      cancelText: 'Cancel',
      danger: true
    });
    if (!ok) return;
    forkJoin(
      ids.map((id) =>
        this.quizSvc.deleteQuiz(id).pipe(
          catchError(() => of({ __failed: true, id }))
        )
      )
    ).subscribe({
      next: (results) => {
        const failed = results.filter((r: any) => r && typeof r === 'object' && r.__failed);
        if (failed.length) {
          this.toast.show(`Some quizzes could not be deleted (${failed.length})`, 'error');
        } else {
          this.toast.show('Selected quizzes deleted', 'success');
        }
        this.selectedIds = [];
        this.load();
        this.cdr.detectChanges();
      },
      error: (e) => {
        this.toast.showHttpError(e, 'Bulk delete could not be completed. Please try again.');
        this.cdr.detectChanges();
      }
    });
  }

  trackByIdx(i: number) {
    return i;
  }

  /** Read-only preview modal (same content as patients see, no answering). */
  previewOpen = false;
  previewLoading = false;
  previewError = '';
  previewQuiz: PreviewQuiz | null = null;
  previewIndex = 0;

  /** English labels for difficulty badges in the preview modal. */
  difficultyLabel(code: string | undefined): string {
    const u = (code || '').toUpperCase();
    if (u === 'EASY') return 'Easy';
    if (u === 'MEDIUM') return 'Medium';
    if (u === 'HARD') return 'Hard';
    return code || '';
  }

  @HostListener('document:keydown', ['$event'])
  onPreviewKeydown(e: KeyboardEvent) {
    if (!this.previewOpen) return;
    if (e.key === 'Escape') {
      e.preventDefault();
      this.closePreview();
    } else if (e.key === 'ArrowLeft') {
      e.preventDefault();
      this.previewPrev();
    } else if (e.key === 'ArrowRight') {
      e.preventDefault();
      this.previewNext();
    }
  }

  openPreview(row: any) {
    if (!row?.id) return;
    this.previewError = '';
    this.previewIndex = 0;
    this.previewOpen = true;

    const cached = this.previewCache.get(row.id);
    if (cached) {
      this.previewQuiz = cached;
      this.previewLoading = false;
      return;
    }

    const fromRow = this.buildPreviewFromRow(row);
    if (fromRow) {
      this.previewQuiz = fromRow;
      this.previewCache.set(row.id, fromRow);
      this.previewLoading = false;
      return;
    }

    this.previewLoading = true;
    this.previewQuiz = null;
    this.quizSvc.getQuizPreviewPayload(row.id).subscribe({
      next: (d: QuizPreviewApi) => {
        const mapped = this.mapPreviewApiToPreview(d);
        this.previewQuiz = mapped;
        this.previewCache.set(row.id, mapped);
        this.previewLoading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.previewLoading = false;
        this.previewError = 'Could not load this quiz.';
        this.cdr.detectChanges();
      }
    });
  }

  private mapPreviewApiToPreview(d: QuizPreviewApi): PreviewQuiz {
    return {
      title: d.title || '',
      description: d.description || '',
      difficulty: d.difficulty || '',
      questions: (d.questions || []).map((q) => ({
        questionText: q.text || '',
        options: [q.optionA || '', q.optionB || '', q.optionC || '', q.optionD || '']
      }))
    };
  }

  /** When the list already includes questions (JOIN FETCH), open without waiting for HTTP. */
  private buildPreviewFromRow(row: any): PreviewQuiz | null {
    const raw = row?.questions;
    if (!Array.isArray(raw) || raw.length === 0) return null;
    const first = raw[0];
    if (first == null) return null;
    if (first.text === undefined && first.questionText === undefined) return null;
    return {
      title: row.title || '',
      description: row.description || '',
      difficulty: row.difficulty || row.level || '',
      questions: raw.map((q: any) => ({
        questionText: q.text || q.questionText || '',
        options:
          Array.isArray(q.options) && q.options.length
            ? q.options
            : [q.optionA || '', q.optionB || '', q.optionC || '', q.optionD || '']
      }))
    };
  }

  closePreview() {
    this.previewOpen = false;
    this.previewQuiz = null;
    this.previewError = '';
    this.previewIndex = 0;
  }

  previewPrev() {
    if (!this.previewQuiz?.questions.length) return;
    if (this.previewIndex > 0) this.previewIndex--;
  }

  previewNext() {
    if (!this.previewQuiz?.questions.length) return;
    if (this.previewIndex < this.previewQuiz.questions.length - 1) this.previewIndex++;
  }

  get previewProgress(): number {
    const n = this.previewQuiz?.questions.length || 0;
    if (!n) return 0;
    return Math.round(((this.previewIndex + 1) / n) * 100);
  }
}
