import { Component, Input, Output, EventEmitter, OnInit, OnDestroy, signal, PLATFORM_ID, Inject, ElementRef, ViewChild, AfterViewInit } from '@angular/core';
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
            @if (isRecording()) {
              <span class="recording-badge">
                <span class="recording-dot"></span>
                Enregistrement
              </span>
            }
          </div>
          <button class="video-call-header__close" (click)="onClose()">
            <ng-icon name="heroXMark" size="24"></ng-icon>
          </button>
        </div>
        <div class="video-call-content" #jitsiContainer></div>
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
  @ViewChild('jitsiContainer', { static: false }) jitsiContainer!: ElementRef;

  @Input() roomName!: string;
  @Input() userName!: string;
  @Input() title = 'Cours d\'échecs';
  @Input() isTeacher = false;
  @Input() jwtToken?: string;
  @Output() closed = new EventEmitter<void>();

  isRecording = signal(false);
  private isBrowser: boolean;
  private api: any = null;
  private recordingStarted = false;

  constructor(
    @Inject(PLATFORM_ID) platformId: Object
  ) {
    this.isBrowser = isPlatformBrowser(platformId);
  }

  ngOnInit(): void {}

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
    const options = {
      roomName: this.roomName,
      parentNode: this.jitsiContainer.nativeElement,
      jwt: this.jwtToken || undefined,
      configOverwrite: {
        prejoinPageEnabled: false,
        startWithAudioMuted: false,
        startWithVideoMuted: false,
        disableDeepLinking: true,
        defaultLanguage: 'fr',
        fileRecordingsEnabled: true,
        localRecording: { enabled: false },
        recordingService: { enabled: true, sharingEnabled: false }
      },
      interfaceConfigOverwrite: {
        SHOW_JITSI_WATERMARK: false,
        SHOW_BRAND_WATERMARK: false,
        DEFAULT_BACKGROUND: '#1e1e24',
        TOOLBAR_BUTTONS: [
          'camera', 'chat', 'closedcaptions', 'desktop', 'fullscreen',
          'fodeviceselection', 'hangup', 'microphone', 'participants-pane',
          'profile', 'raisehand', 'recording', 'security', 'select-background',
          'settings', 'tileview', 'videoquality'
        ]
      },
      userInfo: {
        displayName: this.userName
      }
    };

    console.log('[Jitsi] Initializing with options:', { ...options, jwt: options.jwt ? '[HIDDEN]' : undefined });

    this.api = new JitsiMeetExternalAPI('meet.mychess.fr', options);

    // Event listeners
    this.api.addListener('videoConferenceJoined', (data: any) => {
      console.log('[Jitsi] Conference joined:', data);

      // Auto-start recording if teacher (with delay to ensure connection is stable)
      if (this.isTeacher && !this.recordingStarted) {
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
      }
    });

    this.api.addListener('readyToClose', () => {
      console.log('[Jitsi] Ready to close');
      this.onClose();
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

  onClose(): void {
    if (this.api) {
      // Stop recording before closing if it's running
      if (this.isRecording()) {
        try {
          this.api.executeCommand('stopRecording', 'file');
        } catch (err) {
          console.warn('[Jitsi] Could not stop recording:', err);
        }
      }
      this.api.dispose();
      this.api = null;
    }
    this.closed.emit();
  }
}
