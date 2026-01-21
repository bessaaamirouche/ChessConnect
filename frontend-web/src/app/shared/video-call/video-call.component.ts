import { Component, Input, Output, EventEmitter, OnInit, OnDestroy, signal, PLATFORM_ID, Inject } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { NgIconComponent, provideIcons } from '@ng-icons/core';
import { heroXMark, heroVideoCamera } from '@ng-icons/heroicons/outline';

declare var JitsiMeetExternalAPI: any;

@Component({
  selector: 'app-video-call',
  standalone: true,
  imports: [NgIconComponent],
  viewProviders: [provideIcons({ heroXMark, heroVideoCamera })],
  template: `
    <div class="video-call-overlay" (click)="onClose()">
      <div class="video-call-container" (click)="$event.stopPropagation()">
        <div class="video-call-header">
          <div class="video-call-header__info">
            <ng-icon name="heroVideoCamera" size="20"></ng-icon>
            <span>{{ title }}</span>
          </div>
          <button class="video-call-header__close" (click)="onClose()">
            <ng-icon name="heroXMark" size="24"></ng-icon>
          </button>
        </div>
        <div class="video-call-content">
          @if (loading()) {
            <div class="video-call-loading">
              <span class="spinner spinner--lg"></span>
              <p>Connexion en cours...</p>
            </div>
          }
          @if (error()) {
            <div class="video-call-error">
              <p>{{ error() }}</p>
              <button class="btn btn--primary" (click)="retry()">Réessayer</button>
            </div>
          }
          <div id="jitsi-container" [style.display]="loading() || error() ? 'none' : 'block'"></div>
        </div>
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
    }

    .video-call-loading,
    .video-call-error {
      position: absolute;
      top: 50%;
      left: 50%;
      transform: translate(-50%, -50%);
      text-align: center;
      color: var(--text-secondary);

      p {
        margin-top: 1rem;
      }
    }

    .video-call-error {
      color: var(--color-error, #ef4444);

      button {
        margin-top: 1rem;
      }
    }

    #jitsi-container {
      width: 100%;
      height: 100%;
    }
  `]
})
export class VideoCallComponent implements OnInit, OnDestroy {
  @Input() roomName!: string;
  @Input() userName!: string;
  @Input() title = 'Cours d\'échecs';
  @Input() isTeacher = false;
  @Output() closed = new EventEmitter<void>();

  loading = signal(true);
  error = signal<string | null>(null);
  private api: any;
  private isBrowser: boolean;
  private loadTimeout: any;

  constructor(@Inject(PLATFORM_ID) platformId: Object) {
    this.isBrowser = isPlatformBrowser(platformId);
  }

  ngOnInit(): void {
    if (this.isBrowser) {
      this.loadJitsiScript();
    }
  }

  ngOnDestroy(): void {
    if (this.loadTimeout) {
      clearTimeout(this.loadTimeout);
    }
    if (this.api) {
      this.api.dispose();
    }
  }

  private loadJitsiScript(): void {
    // Timeout after 30 seconds
    this.loadTimeout = setTimeout(() => {
      if (this.loading()) {
        console.error('[Jitsi] Connection timeout after 30s');
        this.loading.set(false);
        this.error.set('La connexion a échoué. Vérifiez votre connexion internet.');
      }
    }, 30000);

    if (typeof JitsiMeetExternalAPI !== 'undefined') {
      this.initJitsi();
      return;
    }

    const script = document.createElement('script');
    script.src = 'https://meet.mychess.fr/external_api.js';
    script.async = true;
    script.onload = () => this.initJitsi();
    script.onerror = () => {
      this.loading.set(false);
      this.error.set('Impossible de charger le service vidéo.');
    };
    document.head.appendChild(script);
  }

  private initJitsi(): void {
    const domain = 'meet.mychess.fr';
    const options = {
      roomName: `mychess_${this.roomName}`,
      parentNode: document.getElementById('jitsi-container'),
      userInfo: {
        displayName: this.userName,
        moderator: this.isTeacher
      },
      configOverwrite: {
        startWithAudioMuted: false,
        startWithVideoMuted: false,
        prejoinPageEnabled: false,
        disableDeepLinking: true,
        defaultLanguage: 'fr'
      },
      interfaceConfigOverwrite: {
        TOOLBAR_BUTTONS: [
          'microphone', 'camera', 'desktop', 'fullscreen',
          'fodeviceselection', 'hangup', 'chat', 'settings',
          'videoquality', 'tileview'
        ],
        SETTINGS_SECTIONS: ['devices', 'language'],
        SHOW_JITSI_WATERMARK: false,
        SHOW_BRAND_WATERMARK: false,
        BRAND_WATERMARK_LINK: '',
        DEFAULT_BACKGROUND: '#1e1e24',
        DISABLE_JOIN_LEAVE_NOTIFICATIONS: true
      }
    };

    console.log('[Jitsi] Creating API with room:', options.roomName);
    this.api = new JitsiMeetExternalAPI(domain, options);

    this.api.addEventListener('videoConferenceJoined', (event: any) => {
      console.log('[Jitsi] Conference joined:', event);
      if (this.loadTimeout) {
        clearTimeout(this.loadTimeout);
      }
      this.loading.set(false);

      // Auto-start recording
      setTimeout(() => {
        console.log('[Jitsi] Starting automatic recording...');
        this.api.executeCommand('startRecording', {
          mode: 'file'
        });
      }, 2000);
    });

    this.api.addEventListener('recordingStatusChanged', (event: any) => {
      console.log('[Jitsi] Recording status changed:', event);
    });

    this.api.addEventListener('participantJoined', (event: any) => {
      console.log('[Jitsi] Participant joined:', event);
    });

    this.api.addEventListener('videoConferenceLeft', () => {
      console.log('[Jitsi] Conference left');
      this.onClose();
    });

    this.api.addEventListener('readyToClose', () => {
      console.log('[Jitsi] Ready to close');
      this.onClose();
    });

    this.api.addEventListener('errorOccurred', (event: any) => {
      console.error('[Jitsi] Error:', event);
      this.loading.set(false);
      this.error.set('Une erreur est survenue lors de la connexion.');
    });

    // Additional debug events
    this.api.addEventListener('browserSupport', (event: any) => {
      console.log('[Jitsi] Browser support:', event);
    });

    this.api.addEventListener('log', (event: any) => {
      if (event.logLevel === 'error') {
        console.error('[Jitsi Log]', event);
      }
    });
  }

  onClose(): void {
    if (this.api) {
      this.api.dispose();
    }
    this.closed.emit();
  }

  retry(): void {
    this.error.set(null);
    this.loading.set(true);
    this.loadJitsiScript();
  }
}
