import { Component, OnInit, OnDestroy, signal, PLATFORM_ID, ElementRef, AfterViewInit, inject, input, output, viewChild } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { NgIconComponent, provideIcons } from '@ng-icons/core';
import { heroXMark, heroVideoCamera, heroPhone } from '@ng-icons/heroicons/outline';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { ToastService } from '../../core/services/toast.service';

declare var JitsiMeetExternalAPI: any;

@Component({
    selector: 'app-video-call',
    imports: [NgIconComponent, TranslateModule],
    viewProviders: [provideIcons({ heroXMark, heroVideoCamera, heroPhone })],
    template: `
    <div class="video-call-overlay">
      <div class="video-call-container" role="dialog" aria-modal="true" aria-labelledby="video-call-title" (click)="$event.stopPropagation()">
        <div class="video-call-header">
          <div class="video-call-header__info">
            <ng-icon name="heroVideoCamera" size="20" aria-hidden="true"></ng-icon>
            <span id="video-call-title">{{ title() }}</span>
            @if (isRecording()) {
              <span class="recording-badge" aria-live="polite">
                <span class="recording-dot" aria-hidden="true"></span>
                {{ 'videoCallLabels.recording' | translate }}
              </span>
            }
          </div>
          <div class="video-call-header__actions">
            <button class="video-call-header__close" (click)="onClose()" [title]="'videoCallLabels.leaveCall' | translate" [attr.aria-label]="'videoCallLabels.closeCall' | translate">
              <ng-icon name="heroXMark" size="24" aria-hidden="true"></ng-icon>
            </button>
          </div>
        </div>
        @if (hasLeft()) {
          <div class="video-call-rejoin">
            <div class="video-call-rejoin__content">
              <ng-icon name="heroPhone" size="48" class="video-call-rejoin__icon"></ng-icon>
              <h3 class="video-call-rejoin__title">{{ 'videoCallLabels.leftCall' | translate }}</h3>
              <p class="video-call-rejoin__subtitle">{{ 'videoCallLabels.lessonInProgress' | translate }}</p>
              <button class="video-call-rejoin__btn" (click)="rejoin()">
                <ng-icon name="heroVideoCamera" size="20"></ng-icon>
                {{ 'videoCallLabels.rejoinCall' | translate }}
              </button>
            </div>
          </div>
        }
        <div class="video-call-content" [class.hidden]="hasLeft()" #jitsiContainer></div>
      </div>
    </div>
  `,
    styles: [`
    .video-call-overlay {
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: rgba(0, 0, 0, 0.9);
      z-index: 1000;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 1rem;
    }

    .video-call-container {
      width: 100%;
      max-width: 1200px;
      height: 90vh;
      background: var(--bg-secondary);
      border-radius: var(--radius-lg);
      overflow: hidden;
      display: flex;
      flex-direction: column;
      border: 1px solid var(--border-default);
    }

    .video-call-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 1rem 1.5rem;
      background: var(--bg-tertiary);
      border-bottom: 1px solid var(--border-subtle);

      &__info {
        display: flex;
        align-items: center;
        gap: 0.75rem;
        color: var(--text-primary);
        font-weight: 600;
      }

      &__actions {
        display: flex;
        align-items: center;
        gap: 0.75rem;
      }

      &__close {
        background: none;
        border: none;
        color: var(--text-muted);
        cursor: pointer;
        padding: 0.5rem;
        border-radius: var(--radius-md);
        transition: all var(--transition-fast);

        &:hover {
          background: var(--bg-elevated);
          color: var(--text-primary);
        }
      }
    }

    .video-call-content {
      flex: 1;
      position: relative;
      background: #000;

      &.hidden {
        display: none;
      }
    }

    .video-call-rejoin {
      flex: 1;
      display: flex;
      align-items: center;
      justify-content: center;
      background: #0d0d0f;

      &__content {
        text-align: center;
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 1rem;
      }

      &__icon {
        color: var(--text-muted);
        opacity: 0.5;
      }

      &__title {
        font-size: 1.25rem;
        font-weight: 600;
        color: var(--text-primary);
        margin: 0;
      }

      &__subtitle {
        font-size: 0.875rem;
        color: var(--text-secondary);
        margin: 0;
      }

      &__btn {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        margin-top: 0.5rem;
        padding: 0.75rem 1.5rem;
        background: var(--gold-400, #d4a84b);
        color: #000;
        border: none;
        border-radius: var(--radius-md);
        font-size: 0.9375rem;
        font-weight: 600;
        cursor: pointer;
        transition: all 0.2s ease;

        &:hover {
          background: var(--gold-300, #e0b85c);
          transform: translateY(-1px);
          box-shadow: 0 4px 12px rgba(212, 168, 75, 0.3);
        }
      }
    }

    .recording-badge {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      background: rgba(239, 68, 68, 0.2);
      color: #ef4444;
      padding: 0.25rem 0.75rem;
      border-radius: var(--radius-full);
      font-size: 0.75rem;
      font-weight: 600;
      margin-left: 1rem;
    }

    .recording-dot {
      width: 8px;
      height: 8px;
      background: #ef4444;
      border-radius: 50%;
      animation: pulse 1.5s infinite;
    }

    @keyframes pulse {
      0%, 100% { opacity: 1; }
      50% { opacity: 0.5; }
    }

  `]
})
export class VideoCallComponent implements OnInit, OnDestroy, AfterViewInit {
  readonly jitsiContainer = viewChild.required<ElementRef>('jitsiContainer');

  readonly roomName = input.required<string>();
  readonly userName = input.required<string>();
  readonly title = input('Cours d\'échecs');
  readonly isTeacher = input(false);
  readonly recordingEnabled = input(false);
  readonly jwtToken = input<string>();
  readonly durationMinutes = input(60);
  readonly closed = output<void>();

  isRecording = signal(false);
  hasLeft = signal(false);
  private platformId = inject(PLATFORM_ID);
  private isBrowser = isPlatformBrowser(this.platformId);
  private api: any = null;
  private recordingStarted = false;
  private toastService = inject(ToastService);
  private translateService = inject(TranslateService);

  ngOnInit(): void {
    if (this.isBrowser) {
      document.body.classList.add('video-call-active');
    }
  }

  ngAfterViewInit(): void {
    if (this.isBrowser) {
      this.loadJitsiScript().then(() => {
        this.initJitsiMeet();
      }).catch(err => {
        console.error('[Jitsi] Failed to load script:', err);
      });
    }
  }

  ngOnDestroy(): void {
    if (this.api) {
      this.api.dispose();
      this.api = null;
    }
    if (this.isBrowser) {
      document.body.classList.remove('video-call-active');
    }
  }

  private loadJitsiScript(): Promise<void> {
    return new Promise((resolve, reject) => {
      if (typeof JitsiMeetExternalAPI !== 'undefined') {
        resolve();
        return;
      }

      const script = document.createElement('script');
      script.src = 'https://meet.mychess.fr/external_api.js';
      script.async = true;
      script.onload = () => resolve();
      script.onerror = () => reject(new Error('Failed to load Jitsi script'));
      document.head.appendChild(script);
    });
  }

  private initJitsiMeet(): void {
    this.hasLeft.set(false);

    const options = {
      roomName: this.roomName(),
      parentNode: this.jitsiContainer().nativeElement,
      jwt: this.jwtToken() || undefined,
      configOverwrite: {
        prejoinPageEnabled: false,
        startWithAudioMuted: false,
        startWithVideoMuted: false,
        disableDeepLinking: true,
        defaultLanguage: 'fr',
        fileRecordingsEnabled: true,
        localRecording: { enabled: false },
        recordingService: { enabled: true, sharingEnabled: false },
        // Disable Jitsi notifications to use our own
        notifications: [],
        disableModeratorIndicator: true
      },
      interfaceConfigOverwrite: {
        SHOW_JITSI_WATERMARK: false,
        SHOW_BRAND_WATERMARK: false,
        DEFAULT_BACKGROUND: '#1e1e24',
        TOOLBAR_BUTTONS: [
          'camera', 'closedcaptions', 'desktop', 'fullscreen',
          'fodeviceselection', 'microphone', 'participants-pane',
          'profile', 'raisehand', 'recording', 'security', 'select-background',
          'settings', 'tileview', 'videoquality'
        ]
      },
      userInfo: {
        displayName: this.userName()
      }
    };

    console.log('[Jitsi] Initializing with options:', { ...options, jwt: options.jwt ? '[HIDDEN]' : undefined });

    this.api = new JitsiMeetExternalAPI('meet.mychess.fr', options);

    // Event listeners
    this.api.addListener('videoConferenceJoined', (data: any) => {
      console.log('[Jitsi] Conference joined:', data);

      // Auto-start recording if teacher and recording is enabled (student is premium)
      if (this.isTeacher() && this.recordingEnabled() && !this.recordingStarted) {
        setTimeout(() => {
          this.startRecording();
        }, 3000);
      }
    });

    this.api.addListener('recordingStatusChanged', (status: any) => {
      console.log('[Jitsi] Recording status changed:', status);
      this.isRecording.set(status.on === true);

      if (status.on) {
        this.recordingStarted = true;
        this.toastService.show(this.translateService.instant('videoCallLabels.recordingStarted'), 'info');
      }
    });

    this.api.addListener('readyToClose', () => {
      console.log('[Jitsi] Ready to close - showing rejoin screen');
      // Don't close the modal, show rejoin screen instead
      if (this.api) {
        this.api.dispose();
        this.api = null;
      }
      this.isRecording.set(false);
      this.hasLeft.set(true);
    });

    this.api.addListener('errorOccurred', (error: any) => {
      console.error('[Jitsi] Error:', error);
    });
  }

  private startRecording(): void {
    if (!this.api || this.recordingStarted) return;

    console.log('[Jitsi] Starting recording automatically...');

    try {
      this.api.executeCommand('startRecording', {
        mode: 'file'
      });
    } catch (err) {
      console.error('[Jitsi] Failed to start recording:', err);
    }
  }

  rejoin(): void {
    // Clear the container and reinitialize Jitsi
    const jitsiContainer = this.jitsiContainer();
    if (jitsiContainer?.nativeElement) {
      jitsiContainer.nativeElement.innerHTML = '';
    }
    this.initJitsiMeet();
  }

  onClose(): void {
    if (this.api) {
      this.api.dispose();
      this.api = null;
    }
    // TODO: The 'emit' function requires a mandatory void argument
    this.closed.emit();
  }
}
