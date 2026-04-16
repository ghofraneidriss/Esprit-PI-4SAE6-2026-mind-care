import { Injectable, NgZone } from '@angular/core';
import { Observable, Subject } from 'rxjs';

export interface SpeechResult {
  transcript: string;
  confidence: number;
  isFinal: boolean;
}

@Injectable({ providedIn: 'root' })
export class SpeechService {
  private recognition: any = null;
  private isListening = false;
  private results$ = new Subject<SpeechResult>();
  private error$ = new Subject<string>();
  private state$ = new Subject<'listening' | 'stopped' | 'error'>();

  readonly isSupported: boolean;

  constructor(private zone: NgZone) {
    const SpeechRecognitionApi =
      typeof window !== 'undefined'
        ? (window as unknown as { SpeechRecognition?: new () => any; webkitSpeechRecognition?: new () => any })
            .SpeechRecognition ||
          (window as unknown as { webkitSpeechRecognition?: new () => any }).webkitSpeechRecognition
        : null;
    this.isSupported = !!SpeechRecognitionApi;

    if (this.isSupported && SpeechRecognitionApi) {
      this.recognition = new SpeechRecognitionApi();
      this.recognition.lang = 'en-US';
      this.recognition.continuous = false;
      this.recognition.interimResults = true;
      this.recognition.maxAlternatives = 3;

      this.recognition.onresult = (event: any) => {
        this.zone.run(() => {
          const result = event.results[event.results.length - 1];
          const transcript = result[0].transcript.trim();
          const confidence = result[0].confidence;
          this.results$.next({ transcript, confidence, isFinal: result.isFinal });
        });
      };

      this.recognition.onerror = (event: any) => {
        this.zone.run(() => {
          this.isListening = false;
          if (event.error !== 'aborted' && event.error !== 'no-speech') {
            this.error$.next(event.error);
            this.state$.next('error');
          } else {
            this.state$.next('stopped');
          }
        });
      };

      this.recognition.onend = () => {
        this.zone.run(() => {
          this.isListening = false;
          this.state$.next('stopped');
        });
      };
    }
  }

  start(lang = 'en-US'): void {
    if (!this.recognition || this.isListening) return;
    this.recognition.lang = lang;
    try {
      this.recognition.start();
      this.isListening = true;
      this.state$.next('listening');
    } catch {
      /* noop */
    }
  }

  stop(): void {
    if (!this.recognition || !this.isListening) return;
    try {
      this.recognition.stop();
    } catch {
      /* noop */
    }
    this.isListening = false;
    this.state$.next('stopped');
  }

  toggle(lang = 'en-US'): void {
    if (this.isListening) {
      this.stop();
    } else {
      this.start(lang);
    }
  }

  get onResult(): Observable<SpeechResult> {
    return this.results$.asObservable();
  }

  get onError(): Observable<string> {
    return this.error$.asObservable();
  }

  get onStateChange(): Observable<'listening' | 'stopped' | 'error'> {
    return this.state$.asObservable();
  }

  get listening(): boolean {
    return this.isListening;
  }

  matchAnswer(transcript: string, options: string[]): number {
    const spoken = this.normalize(transcript);
    const letterMap: Record<string, number> = { a: 0, b: 1, c: 2, d: 3 };
    if (letterMap[spoken] !== undefined && letterMap[spoken] < options.length) {
      return letterMap[spoken];
    }
    const frLetters: Record<string, number> = {
      ah: 0,
      ha: 0,
      hein: 0,
      bé: 1,
      be: 1,
      bay: 1,
      cé: 2,
      ce: 2,
      say: 2,
      sais: 2,
      dé: 3,
      de: 3,
      day: 3,
    };
    if (frLetters[spoken] !== undefined && frLetters[spoken] < options.length) {
      return frLetters[spoken];
    }
    const prefixMatch = spoken.match(
      /^(?:option|answer|réponse|reponse|choix|choice|letter)\s+(a|b|c|d)$/
    );
    if (prefixMatch) {
      const idx = letterMap[prefixMatch[1]];
      if (idx !== undefined && idx < options.length) return idx;
    }
    const numMap: Record<string, number> = {
      un: 0,
      une: 0,
      '1': 0,
      premier: 0,
      première: 0,
      premiere: 0,
      deux: 1,
      '2': 1,
      deuxième: 1,
      deuxieme: 1,
      second: 1,
      seconde: 1,
      trois: 2,
      '3': 2,
      troisième: 2,
      troisieme: 2,
      quatre: 3,
      '4': 3,
      quatrième: 3,
      quatrieme: 3,
    };
    if (numMap[spoken] !== undefined && numMap[spoken] < options.length) {
      return numMap[spoken];
    }
    let bestIdx = -1;
    let bestScore = 0;
    for (let i = 0; i < options.length; i++) {
      const optNorm = this.normalize(options[i]);
      if (spoken === optNorm) return i;
      if (optNorm.includes(spoken) || spoken.includes(optNorm)) {
        const score =
          Math.min(spoken.length, optNorm.length) / Math.max(spoken.length, optNorm.length);
        if (score > bestScore) {
          bestScore = score;
          bestIdx = i;
        }
      }
      const spokenWords = spoken.split(/\s+/);
      const optWords = optNorm.split(/\s+/);
      const overlap = spokenWords.filter((w) => optWords.includes(w)).length;
      const overlapScore = overlap / Math.max(spokenWords.length, optWords.length);
      if (overlapScore > bestScore) {
        bestScore = overlapScore;
        bestIdx = i;
      }
    }
    return bestScore >= 0.3 ? bestIdx : -1;
  }

  private normalize(text: string): string {
    return text
      .toLowerCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .replace(/[^a-z0-9\s]/g, '')
      .replace(/\s+/g, ' ')
      .trim();
  }
}
