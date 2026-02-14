import { Component, OnDestroy, signal, computed, output, inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NgIconComponent, provideIcons } from '@ng-icons/core';
import { heroPlay, heroPause, heroArrowPath, heroXMark } from '@ng-icons/heroicons/outline';
import { TranslateModule } from '@ngx-translate/core';

type TimerState = 'setup' | 'running' | 'paused' | 'finished';

@Component({
    selector: 'app-call-timer',
    imports: [NgIconComponent, TranslateModule, FormsModule],
    viewProviders: [provideIcons({ heroPlay, heroPause, heroArrowPath, heroXMark })],
    template: `
    @if (timerState() === 'setup') {
      <div class="call-timer" role="region" [attr.aria-label]="'videoCallLabels.timer' | translate">
        <div class="call-timer__header">
          <span class="call-timer__title">{{ 'videoCallLabels.timerTitle' | translate }}</span>
          <button class="call-timer__close" (click)="timerClosed.emit()"
                  [attr.aria-label]="'common.close' | translate">
            <ng-icon name="heroXMark" size="16" aria-hidden="true"></ng-icon>
          </button>
        </div>

        <div class="call-timer__presets">
          @for (preset of presets; track preset) {
            <button class="call-timer__preset-btn" (click)="selectPreset(preset)">
              {{ preset }} {{ 'videoCallLabels.timerMinutesShort' | translate }}
            </button>
          }
        </div>

        <div class="call-timer__custom">
          <input type="number"
                 class="call-timer__input"
                 [(ngModel)]="customMinutes"
                 min="1" max="120"
                 [placeholder]="'videoCallLabels.timerCustomPlaceholder' | translate"
                 (keyup.enter)="setCustomDuration()"
                 [attr.aria-label]="'videoCallLabels.timerCustomPlaceholder' | translate" />
          <button class="call-timer__start-btn"
                  (click)="setCustomDuration()"
                  [disabled]="!customMinutes || customMinutes < 1">
            {{ 'videoCallLabels.timerStart' | translate }}
          </button>
        </div>
      </div>
    } @else {
      <div class="call-timer call-timer--active"
           [class.call-timer--flashing]="timerState() === 'finished'"
           role="timer"
           aria-live="polite"
           [attr.aria-label]="'videoCallLabels.timerCountdown' | translate">

        <svg class="call-timer__ring" viewBox="0 0 36 36" aria-hidden="true">
          <circle class="call-timer__ring-bg" cx="18" cy="18" r="16"
                  fill="none" stroke-width="2" />
          <circle class="call-timer__ring-progress" cx="18" cy="18" r="16"
                  fill="none" stroke-width="2.5"
                  stroke-linecap="round"
                  [attr.stroke-dasharray]="circumference"
                  [attr.stroke-dashoffset]="circumference - (circumference * progressPercent() / 100)" />
        </svg>

        <span class="call-timer__display">{{ displayTime() }}</span>

        @if (timerState() === 'finished') {
          <span class="call-timer__finished-label">{{ 'videoCallLabels.timerFinished' | translate }}</span>
        }

        <div class="call-timer__controls">
          @if (timerState() === 'running') {
            <button class="call-timer__ctrl-btn" (click)="pauseTimer()"
                    [attr.aria-label]="'videoCallLabels.timerPause' | translate"
                    [title]="'videoCallLabels.timerPause' | translate">
              <ng-icon name="heroPause" size="14" aria-hidden="true"></ng-icon>
            </button>
          }
          @if (timerState() === 'paused') {
            <button class="call-timer__ctrl-btn" (click)="resumeTimer()"
                    [attr.aria-label]="'videoCallLabels.timerResume' | translate"
                    [title]="'videoCallLabels.timerResume' | translate">
              <ng-icon name="heroPlay" size="14" aria-hidden="true"></ng-icon>
            </button>
          }
          <button class="call-timer__ctrl-btn" (click)="resetTimer()"
                  [attr.aria-label]="'videoCallLabels.timerReset' | translate"
                  [title]="'videoCallLabels.timerReset' | translate">
            <ng-icon name="heroArrowPath" size="14" aria-hidden="true"></ng-icon>
          </button>
        </div>
      </div>
    }
  `,
    styles: [`
    .call-timer {
      position: absolute;
      bottom: 1rem;
      right: 1rem;
      z-index: 10;
      background: rgba(22, 22, 26, 0.95);
      backdrop-filter: blur(16px);
      -webkit-backdrop-filter: blur(16px);
      border: 1px solid var(--border-default, rgba(255, 255, 255, 0.12));
      border-radius: 12px;
      padding: 1rem;
      width: 220px;
      box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4);
      animation: timerSlideIn 0.2s cubic-bezier(0.16, 1, 0.3, 1);
    }

    .call-timer__header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 0.75rem;
    }

    .call-timer__title {
      font-size: 0.8125rem;
      font-weight: 600;
      color: var(--text-primary, #fff);
    }

    .call-timer__close {
      background: none;
      border: none;
      color: var(--text-muted, #888);
      cursor: pointer;
      padding: 0.25rem;
      border-radius: 6px;
      display: flex;
      align-items: center;
      transition: all 0.15s ease;

      &:hover {
        background: rgba(255, 255, 255, 0.08);
        color: var(--text-primary, #fff);
      }
    }

    .call-timer__presets {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 0.5rem;
      margin-bottom: 0.75rem;
    }

    .call-timer__preset-btn {
      padding: 0.5rem;
      background: var(--bg-tertiary, #1e1e24);
      border: 1px solid var(--border-subtle, rgba(255, 255, 255, 0.06));
      border-radius: 8px;
      color: var(--text-primary, #fff);
      font-size: 0.8125rem;
      font-weight: 500;
      cursor: pointer;
      transition: all 0.15s ease;

      &:hover {
        border-color: var(--gold-400, #d4a84b);
        background: rgba(212, 168, 75, 0.1);
        color: var(--gold-300, #ffd666);
      }

      &:active {
        transform: scale(0.96);
      }
    }

    .call-timer__custom {
      display: flex;
      gap: 0.5rem;
    }

    .call-timer__input {
      flex: 1;
      min-width: 0;
      padding: 0.4375rem 0.625rem;
      background: var(--bg-tertiary, #1e1e24);
      border: 1px solid var(--border-subtle, rgba(255, 255, 255, 0.06));
      border-radius: 8px;
      color: var(--text-primary, #fff);
      font-size: 0.8125rem;
      outline: none;
      transition: border-color 0.15s ease;
      -moz-appearance: textfield;

      &::-webkit-inner-spin-button,
      &::-webkit-outer-spin-button {
        -webkit-appearance: none;
        margin: 0;
      }

      &::placeholder {
        color: var(--text-muted, #888);
        font-size: 0.75rem;
      }

      &:focus {
        border-color: var(--gold-400, #d4a84b);
      }
    }

    .call-timer__start-btn {
      padding: 0.4375rem 0.75rem;
      background: var(--gold-400, #d4a84b);
      color: #000;
      border: none;
      border-radius: 8px;
      font-size: 0.8125rem;
      font-weight: 600;
      cursor: pointer;
      white-space: nowrap;
      transition: all 0.15s ease;

      &:hover:not(:disabled) {
        background: var(--gold-300, #e0b85c);
      }

      &:disabled {
        opacity: 0.4;
        cursor: not-allowed;
      }
    }

    /* Active timer display */
    .call-timer--active {
      width: auto;
      min-width: 90px;
      padding: 0.75rem;
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 0.375rem;
    }

    .call-timer__ring {
      width: 52px;
      height: 52px;
      transform: rotate(-90deg);
    }

    .call-timer__ring-bg {
      stroke: var(--border-subtle, rgba(255, 255, 255, 0.08));
    }

    .call-timer__ring-progress {
      stroke: var(--gold-400, #d4a84b);
      transition: stroke-dashoffset 1s linear;
    }

    .call-timer__display {
      font-family: 'JetBrains Mono', 'SF Mono', 'Fira Code', monospace;
      font-size: 1.25rem;
      font-weight: 600;
      color: var(--gold-300, #ffd666);
      letter-spacing: 0.05em;
    }

    .call-timer__finished-label {
      font-size: 0.6875rem;
      color: #f87171;
      font-weight: 500;
    }

    .call-timer__controls {
      display: flex;
      gap: 0.375rem;
    }

    .call-timer__ctrl-btn {
      background: var(--bg-tertiary, #1e1e24);
      border: 1px solid var(--border-subtle, rgba(255, 255, 255, 0.06));
      border-radius: 6px;
      color: var(--text-secondary, #aaa);
      padding: 0.375rem;
      cursor: pointer;
      display: flex;
      align-items: center;
      transition: all 0.15s ease;

      &:hover {
        color: var(--text-primary, #fff);
        border-color: var(--border-default, rgba(255, 255, 255, 0.12));
      }
    }

    /* Flashing when finished */
    .call-timer--flashing {
      animation: timerFlash 0.6s ease-in-out infinite;
    }

    .call-timer--flashing .call-timer__display {
      color: #f87171;
    }

    .call-timer--flashing .call-timer__ring-progress {
      stroke: #f87171;
    }

    @keyframes timerFlash {
      0%, 100% { border-color: var(--gold-400, #d4a84b); }
      50% { border-color: #f87171; background: rgba(248, 113, 113, 0.08); }
    }

    @keyframes timerSlideIn {
      from { opacity: 0; transform: translateY(8px) scale(0.95); }
      to   { opacity: 1; transform: translateY(0) scale(1); }
    }

    /* Mobile */
    @media (max-width: 480px) {
      .call-timer {
        width: calc(100% - 2rem);
        left: 1rem;
        right: 1rem;
      }

      .call-timer--active {
        width: auto;
        left: auto;
        min-width: 80px;
      }

      .call-timer__display {
        font-size: 1.125rem;
      }

      .call-timer__ring {
        width: 44px;
        height: 44px;
      }
    }
  `]
})
export class CallTimerComponent implements OnDestroy {
  readonly timerClosed = output<void>();

  readonly presets = [1, 3, 5, 10];
  readonly circumference = 2 * Math.PI * 16; // ~100.53

  readonly timerState = signal<TimerState>('setup');
  readonly totalSeconds = signal(60);
  readonly remainingSeconds = signal(60);

  customMinutes = 0;

  readonly displayTime = computed(() => {
    const total = this.remainingSeconds();
    const minutes = Math.floor(total / 60);
    const seconds = total % 60;
    return `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
  });

  readonly progressPercent = computed(() => {
    const total = this.totalSeconds();
    if (total === 0) return 0;
    return ((total - this.remainingSeconds()) / total) * 100;
  });

  private platformId = inject(PLATFORM_ID);
  private isBrowser = isPlatformBrowser(this.platformId);
  private intervalId: ReturnType<typeof setInterval> | null = null;
  private endTime = 0;
  private audioContext: AudioContext | null = null;

  selectPreset(minutes: number): void {
    const seconds = minutes * 60;
    this.totalSeconds.set(seconds);
    this.remainingSeconds.set(seconds);
    this.startTimer();
  }

  setCustomDuration(): void {
    if (!this.customMinutes || this.customMinutes < 1) return;
    const clamped = Math.min(120, Math.max(1, Math.round(this.customMinutes)));
    const seconds = clamped * 60;
    this.totalSeconds.set(seconds);
    this.remainingSeconds.set(seconds);
    this.startTimer();
  }

  startTimer(): void {
    this.initAudioContext();
    this.endTime = Date.now() + this.remainingSeconds() * 1000;
    this.timerState.set('running');
    this.clearInterval();
    this.intervalId = setInterval(() => this.tick(), 250);
  }

  pauseTimer(): void {
    this.clearInterval();
    this.timerState.set('paused');
  }

  resumeTimer(): void {
    this.endTime = Date.now() + this.remainingSeconds() * 1000;
    this.timerState.set('running');
    this.clearInterval();
    this.intervalId = setInterval(() => this.tick(), 250);
  }

  resetTimer(): void {
    this.clearInterval();
    this.timerState.set('setup');
    this.remainingSeconds.set(this.totalSeconds());
  }

  ngOnDestroy(): void {
    this.clearInterval();
    if (this.audioContext) {
      this.audioContext.close().catch(() => {});
      this.audioContext = null;
    }
  }

  private tick(): void {
    const remaining = Math.max(0, Math.ceil((this.endTime - Date.now()) / 1000));
    this.remainingSeconds.set(remaining);

    if (remaining <= 0) {
      this.clearInterval();
      this.timerState.set('finished');
      this.playBeep();
    }
  }

  private clearInterval(): void {
    if (this.intervalId !== null) {
      clearInterval(this.intervalId);
      this.intervalId = null;
    }
  }

  private initAudioContext(): void {
    if (!this.isBrowser || this.audioContext) return;
    try {
      this.audioContext = new (window.AudioContext || (window as any).webkitAudioContext)();
    } catch {
      // Audio not available
    }
  }

  private playBeep(): void {
    if (!this.isBrowser || !this.audioContext) return;

    try {
      const ctx = this.audioContext;
      const now = ctx.currentTime;

      const beeps = [
        { freq: 440, start: 0, duration: 0.15 },
        { freq: 440, start: 0.25, duration: 0.15 },
        { freq: 880, start: 0.5, duration: 0.3 },
      ];

      for (const beep of beeps) {
        const oscillator = ctx.createOscillator();
        const gainNode = ctx.createGain();

        oscillator.type = 'sine';
        oscillator.frequency.value = beep.freq;

        gainNode.gain.setValueAtTime(0, now + beep.start);
        gainNode.gain.linearRampToValueAtTime(0.3, now + beep.start + 0.02);
        gainNode.gain.linearRampToValueAtTime(0, now + beep.start + beep.duration);

        oscillator.connect(gainNode);
        gainNode.connect(ctx.destination);

        oscillator.start(now + beep.start);
        oscillator.stop(now + beep.start + beep.duration + 0.01);
      }
    } catch {
      // Audio playback failed silently
    }
  }
}
