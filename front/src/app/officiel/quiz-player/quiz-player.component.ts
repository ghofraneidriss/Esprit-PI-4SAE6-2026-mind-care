import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { QuizService } from '../../core/services/quiz.service';
import { GameResultService } from '../../core/services/game-result.service';
import { AuthService } from '../../core/services/auth.service';
import { SpeechService } from '../../core/services/speech.service';
import { PhotoService } from '../../core/services/photo.service';
import { resolveActivitiesMediaUrl } from '../../../environments/environment';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-off-quiz-player',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './quiz-player.component.html',
  styleUrls: ['./quiz-player.component.css']
})
export class QuizPlayerComponent implements OnInit, OnDestroy {
  type = '';
  activityId = 0;
  activity: any = null;
  loading = true;
  error = '';

  // Quiz state
  currentQ = 0;
  answers: number[] = [];
  questionTimes: number[] = [];
  startTime = 0;
  totalStartTime = 0;
  timer = 0;
  timerInterval: any;
  finished = false;
  result: any = null;
  saving = false;
  saved = false;
  saveError = '';

  user: any;

  // Voice input state
  voiceSupported = false;
  voiceListening = false;
  voiceTranscript = '';
  voiceMatchFeedback = '';
  voiceFeedbackType: 'success' | 'error' | '' = '';
  private speechSubs: Subscription[] = [];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private quizSvc: QuizService,
    private grSvc: GameResultService,
    private auth: AuthService,
    private photoSvc: PhotoService,
    private cdr: ChangeDetectorRef,
    public speechSvc: SpeechService
  ) {}

  ngOnInit() {
    this.user = this.auth.currentUser;
    this.type = this.route.snapshot.paramMap.get('type') || 'quiz';
    this.activityId = +(this.route.snapshot.paramMap.get('id') || 0);
    this.voiceSupported = this.speechSvc.isSupported;
    this.initVoice();
    this.loadActivity();
  }

  ngOnDestroy() {
    clearInterval(this.timerInterval);
    this.speechSvc.stop();
    this.speechSubs.forEach(s => s.unsubscribe());
  }

  /**
   * Mélange les réponses pour l’identification photo (ordre différent à chaque session).
   */
  private buildShuffledPhotoQuestion(d: any): {
    questionText: string;
    options: string[];
    correctAnswer: number;
    imageUrl: string;
  } {
    const raw = (d.options || []).filter((o: string) => (o || '').trim());
    const opts = raw.length ? [...raw] : ['Option A', 'Option B', 'Option C', 'Option D'];
    for (let i = opts.length - 1; i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1));
      [opts[i], opts[j]] = [opts[j], opts[i]];
    }
    const correctText = d.correctAnswer || '';
    let correctIdx = opts.indexOf(correctText);
    if (correctIdx < 0) {
      correctIdx = 0;
    }
    return {
      questionText: d.description || d.title || 'What do you see in this image?',
      options: opts,
      correctAnswer: correctIdx,
      imageUrl: resolveActivitiesMediaUrl(d.imageUrl)
    };
  }

  loadActivity() {
    this.loading = true;
    if (this.type === 'quiz') {
      this.quizSvc.getQuizById(this.activityId).subscribe({
        next: (d: any) => {
          // Transform backend questions to playable format
          this.activity = {
            ...d,
            questions: (d.questions || []).map((q: any) => {
              const opts = [q.optionA || '', q.optionB || '', q.optionC || '', q.optionD || ''].filter((o: string) => o);
              const ansMap: any = { A: 0, B: 1, C: 2, D: 3 };
              const ca = ansMap[(q.correctAnswer || 'A').toUpperCase()] ?? 0;
              return { questionText: q.text || '', options: opts, correctAnswer: ca };
            })
          };
          if (!this.activity.questions.length) {
            this.error = 'This quiz has no questions';
            this.loading = false;
            this.cdr.detectChanges();
            return;
          }
          this.checkFirstAttemptOrStart();
        },
        error: () => { this.error = 'Quiz not found'; this.loading = false; this.cdr.detectChanges(); }
      });
    } else {
      this.photoSvc.getPhotoById(this.activityId).subscribe({
        next: (d: any) => {
          const q = this.buildShuffledPhotoQuestion(d);
          this.activity = {
            ...d,
            questions: [q]
          };
          this.checkFirstAttemptOrStart();
        },
        error: () => { this.error = 'Activity not found'; this.loading = false; this.cdr.detectChanges(); }
      });
    }
  }

  /**
   * Une seule tentative par quiz / activité photo : le médecin consulte la première impression.
   */
  private checkFirstAttemptOrStart() {
    const uid = this.auth.getUserId();
    if (uid == null) {
      this.initQuiz();
      return;
    }
    const at = this.type === 'quiz' ? 'QUIZ' : 'PHOTO';
    this.grSvc.getResultsByPatient(uid).subscribe({
      next: (results) => {
        const done = (results || []).some(
          (r) => r.activityId === this.activityId && (r.activityType || '').toUpperCase() === at
        );
        if (done) {
          this.error =
            'You have already completed this activity. Your first result is kept for your doctor and cannot be redone.';
          this.loading = false;
          this.activity = null;
          this.cdr.detectChanges();
          return;
        }
        this.initQuiz();
      },
      error: () => this.initQuiz()
    });
  }

  initQuiz() {
    this.loading = false;
    this.answers = new Array(this.activity.questions.length).fill(-1);
    this.questionTimes = new Array(this.activity.questions.length).fill(0);
    this.totalStartTime = Date.now();
    this.startQuestionTimer();
    this.cdr.detectChanges();
  }

  startQuestionTimer() {
    this.startTime = Date.now();
    this.timer = 0;
    clearInterval(this.timerInterval);
    this.timerInterval = setInterval(() => { this.timer = Math.floor((Date.now() - this.startTime) / 1000); }, 100);
  }

  selectAnswer(optIndex: number) {
    if (this.finished) return;
    this.answers[this.currentQ] = optIndex;
    this.questionTimes[this.currentQ] = (Date.now() - this.startTime) / 1000;
  }

  next() {
    if (this.currentQ < this.activity.questions.length - 1) {
      this.currentQ++;
      this.startQuestionTimer();
    }
  }

  prev() {
    if (this.currentQ > 0) {
      this.currentQ--;
      this.startQuestionTimer();
    }
  }

  finish() {
    clearInterval(this.timerInterval);
    this.questionTimes[this.currentQ] = (Date.now() - this.startTime) / 1000;
    this.finished = true;

    const questions = this.activity.questions;
    let correct = 0;
    questions.forEach((q: any, i: number) => {
      if (this.answers[i] === q.correctAnswer) correct++;
    });

    const totalTime = Math.round((Date.now() - this.totalStartTime) / 1000);
    const avgTime = this.questionTimes.length ? this.questionTimes.reduce((a, b) => a + b, 0) / this.questionTimes.length : 0;

    const uid = this.auth.getUserId();
    this.result = {
      patientId: uid ?? 0,
      patientEmail: this.auth.getCurrentUser()?.email || '',
      patientName: this.auth.getFullName(),
      activityType: this.type === 'quiz' ? 'QUIZ' : 'PHOTO',
      activityId: this.activityId,
      activityTitle: this.activity.title,
      score: correct,
      maxScore: questions.length,
      difficulty: this.activity.difficulty || 'MEDIUM',
      totalQuestions: questions.length,
      correctAnswers: correct,
      timeSpentSeconds: totalTime,
      avgResponseTime: Math.round(avgTime * 10) / 10
    };
    this.saveError = '';
    this.persistGameResult();
  }

  retryAutoSave() {
    this.saveError = '';
    this.persistGameResult();
  }

  /** Sauvegarde automatique obligatoire à la fin (pas de bouton « enregistrer »). */
  private persistGameResult() {
    if (!this.result) return;
    this.saving = true;
    this.saved = false;
    this.grSvc.createGameResult(this.result).subscribe({
      next: (r: any) => {
        this.result = { ...this.result, ...r };
        this.saved = true;
        this.saving = false;
        this.cdr.detectChanges();
      },
      error: (e: any) => {
        this.saving = false;
        if (e.status === 403 && e.error?.error === 'QUIZ_LIMIT_REACHED') {
          this.saveError =
            '🚫 You have reached your allowed quiz limit. Contact your doctor to adjust the limit.';
        } else if (e.status === 409 && e.error?.error === 'FIRST_ATTEMPT_EXISTS') {
          this.saveError =
            (e.error?.message as string) ||
            'A result for this activity already exists; the first attempt is kept for medical follow-up.';
        } else {
          this.saveError = 'Could not save the result automatically. Please try again from the activities list.';
        }
        this.cdr.detectChanges();
      }
    });
  }

  get progress(): number {
    return Math.round(((this.currentQ + 1) / this.activity.questions.length) * 100);
  }

  get scorePercent(): number {
    if (!this.result) return 0;
    return Math.round((this.result.correctAnswers / this.result.totalQuestions) * 100);
  }

  getRiskClass(l: string) {
    return { LOW: 'risk-low', MEDIUM: 'risk-med', HIGH: 'risk-high', CRITICAL: 'risk-crit' }[l] || '';
  }

  // ===== Voice Input =====

  initVoice() {
    if (!this.voiceSupported) return;

    this.speechSubs.push(
      this.speechSvc.onResult.subscribe(r => {
        this.voiceTranscript = r.transcript;
        if (r.isFinal && this.activity && !this.finished) {
          const opts = this.activity.questions[this.currentQ].options;
          const matchIdx = this.speechSvc.matchAnswer(r.transcript, opts);
          if (matchIdx >= 0) {
            this.selectAnswer(matchIdx);
            this.voiceMatchFeedback = `✅ "${r.transcript}" → ${['A','B','C','D'][matchIdx]}`;
            this.voiceFeedbackType = 'success';
          } else {
            this.voiceMatchFeedback = `❌ "${r.transcript}" — not recognized, try again`;
            this.voiceFeedbackType = 'error';
          }
        }
        this.cdr.detectChanges();
      }),
      this.speechSvc.onStateChange.subscribe(state => {
        this.voiceListening = state === 'listening';
        if (state === 'stopped') {
          this.voiceTranscript = '';
        }
        this.cdr.detectChanges();
      }),
      this.speechSvc.onError.subscribe(() => {
        this.voiceListening = false;
        this.voiceMatchFeedback = '⚠️ Microphone error — check permissions';
        this.voiceFeedbackType = 'error';
        this.cdr.detectChanges();
      })
    );
  }

  toggleVoice() {
    this.voiceMatchFeedback = '';
    this.voiceFeedbackType = '';
    this.speechSvc.toggle('en-US');
  }
}
