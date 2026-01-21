import { Component, Input, Output, EventEmitter, OnInit, signal, PLATFORM_ID, Inject } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { NgIconComponent, provideIcons } from '@ng-icons/core';
import { heroXMark, heroVideoCamera } from '@ng-icons/heroicons/outline';

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
          @if (iframeUrl()) {
            <iframe
              [src]="iframeUrl()"
              allow="camera; microphone; fullscreen; display-capture; autoplay"
              class="jitsi-iframe"
            ></iframe>
          }
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

    .jitsi-iframe {
      width: 100%;
      height: 100%;
      border: none;
    }
  `]
})
export class VideoCallComponent implements OnInit {
  @Input() roomName!: string;
  @Input() userName!: string;
  @Input() title = 'Cours d\'échecs';
  @Input() isTeacher = false;
  @Input() jwtToken?: string;
  @Output() closed = new EventEmitter<void>();

  iframeUrl = signal<SafeResourceUrl | null>(null);
  private isBrowser: boolean;

  constructor(
    @Inject(PLATFORM_ID) platformId: Object,
    private sanitizer: DomSanitizer
  ) {
    this.isBrowser = isPlatformBrowser(platformId);
  }

  ngOnInit(): void {
    if (this.isBrowser) {
      this.buildIframeUrl();
    }
  }

  private buildIframeUrl(): void {
    // Construire l'URL Jitsi avec les configs
    const baseUrl = `https://meet.mychess.fr/${this.roomName}`;
    const params = new URLSearchParams();

    // Ajouter le JWT si disponible
    if (this.jwtToken) {
      params.set('jwt', this.jwtToken);
    }

    // Configurations Jitsi via URL hash
    const configParams = [
      'config.prejoinPageEnabled=false',
      'config.startWithAudioMuted=false',
      'config.startWithVideoMuted=false',
      'config.disableDeepLinking=true',
      'config.defaultLanguage=fr',
      `userInfo.displayName=${encodeURIComponent(this.userName)}`,
      'interfaceConfig.SHOW_JITSI_WATERMARK=false',
      'interfaceConfig.SHOW_BRAND_WATERMARK=false',
      'interfaceConfig.DEFAULT_BACKGROUND=#1e1e24'
    ];

    const queryString = params.toString();
    const hashString = configParams.join('&');

    let url = baseUrl;
    if (queryString) {
      url += '?' + queryString;
    }
    url += '#' + hashString;

    console.log('[Jitsi] Opening iframe with URL:', url);
    this.iframeUrl.set(this.sanitizer.bypassSecurityTrustResourceUrl(url));
  }

  onClose(): void {
    this.closed.emit();
  }
}
