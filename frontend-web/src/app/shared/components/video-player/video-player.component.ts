import {
  Component,
  ElementRef,
  AfterViewInit,
  OnDestroy,
  OnInit,
  ChangeDetectionStrategy,
  signal,
  computed,
  inject,
  input,
  output,
  viewChild
} from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { NgIconComponent, provideIcons } from '@ng-icons/core';
import { TranslateModule } from '@ngx-translate/core';
import {
  heroXMark,
  heroPlay,
  heroPause,
  heroSpeakerWave,
  heroSpeakerXMark,
  heroArrowsPointingOut,
  heroArrowDownTray,
  heroForward,
  heroBackward,
  heroCog6Tooth
} from '@ng-icons/heroicons/outline';
import { heroPlaySolid } from '@ng-icons/heroicons/solid';
import Hls from 'hls.js';
import { VideoProgressService } from '../../../core/services/video-progress.service';
import { AuthService } from '../../../core/services/auth.service';

@Component({
    selector: 'app-video-player',
    imports: [NgIconComponent, TranslateModule],
    viewProviders: [provideIcons({
            heroXMark,
            heroPlay,
            heroPause,
            heroSpeakerWave,
            heroSpeakerXMark,
            heroArrowsPointingOut,
            heroArrowDownTray,
            heroForward,
            heroBackward,
            heroPlaySolid,
            heroCog6Tooth
        })],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './video-player.component.html',
    styleUrl: './video-player.component.scss'
})
export class VideoPlayerComponent implements OnInit, AfterViewInit, OnDestroy {
  readonly url = input.required<string>();
  readonly videoId = input<string | number | null>(); // Unique identifier for saving progress
  readonly title = input('Enregistrement du cours');
  readonly teacherName = input<string>();
  readonly lessonDate = input<string>();
  readonly downloadUrl = input<string>();
  readonly close = output<void>();

  private readonly PROGRESS_STORAGE_KEY = 'video_progress_';
  private readonly SAVE_INTERVAL = 5000; // Save every 5 seconds
  private readonly RESUME_THRESHOLD = 10; // Don't save if less than 10 seconds
  private readonly COMPLETION_THRESHOLD = 0.95; // Consider complete at 95%
  private saveProgressInterval: any;

  readonly videoElement = viewChild<ElementRef<HTMLVideoElement>>('videoElement');
  readonly playerContainer = viewChild<ElementRef<HTMLDivElement>>('playerContainer');

  // Signals for reactive state
  isPlaying = signal(false);
  isMuted = signal(false);
  isFullscreen = signal(false);
  currentTime = signal(0);
  duration = signal(0);
  volume = signal(1);
  isLoading = signal(true);
  hasError = signal(false);
  showControls = signal(true);
  savedProgressTime = signal<number | null>(null); // For resume indicator

  // Quality settings
  availableQualities = signal<{height: number, label: string, index: number}[]>([]);
  currentQuality = signal<number>(-1); // -1 = auto
  showQualityMenu = signal(false);

  // Computed values
  progress = computed(() => {
    const dur = this.duration();
    return dur > 0 ? (this.currentTime() / dur) * 100 : 0;
  });

  formattedCurrentTime = computed(() => this.formatTime(this.currentTime()));
  formattedDuration = computed(() => this.formatTime(this.duration()));

  // Bunny Stream detection
  isBunnyStream = signal(false);
  bunnyEmbedUrl = signal<SafeResourceUrl | null>(null);

  private hls: Hls | null = null;
  private controlsTimeout: any;

  private sanitizer = inject(DomSanitizer);
  private videoProgressService = inject(VideoProgressService);
  private authService = inject(AuthService);

  ngOnInit(): void {
    this.detectBunnyStream();
    this.checkSavedProgress();
  }

  private checkSavedProgress(): void {
    const lessonId = this.getNumericLessonId();

    // If authenticated, fetch from backend first
    if (lessonId && this.authService.isAuthenticated()) {
      this.videoProgressService.getProgress(lessonId).subscribe(progress => {
        if (progress.watchPosition && progress.watchPosition > this.RESUME_THRESHOLD && !progress.completed) {
          this.savedProgressTime.set(progress.watchPosition);
        }
      });
      return;
    }

    // Fallback to localStorage
    const key = this.getStorageKey();
    if (!key) return;

    try {
      const savedData = localStorage.getItem(key);
      if (savedData) {
        const progressData = JSON.parse(savedData);
        if (progressData.time && progressData.time > this.RESUME_THRESHOLD) {
          this.savedProgressTime.set(progressData.time);
        }
      }
    } catch {
      // Ignore invalid data
    }
  }

  private getNumericLessonId(): number | null {
    const videoId = this.videoId();
    if (!videoId) return null;
    const id = typeof videoId === 'string' ? parseInt(videoId, 10) : videoId;
    return isNaN(id) ? null : id;
  }

  ngAfterViewInit(): void {
    if (!this.isBunnyStream()) {
      this.initializePlayer();
    }
    this.setupKeyboardControls();
    this.setupFullscreenListeners();
  }

  ngOnDestroy(): void {
    // Save progress before destroying
    this.saveProgress();
    this.destroyPlayer();
    if (this.controlsTimeout) {
      clearTimeout(this.controlsTimeout);
    }
    if (this.saveProgressInterval) {
      clearInterval(this.saveProgressInterval);
    }
    document.removeEventListener('keydown', this.handleKeydown);
    this.removeFullscreenListeners();
  }

  private setupFullscreenListeners(): void {
    // Listen for fullscreen changes
    document.addEventListener('fullscreenchange', this.onFullscreenChange);
    document.addEventListener('webkitfullscreenchange', this.onFullscreenChange);

    // iOS video fullscreen events
    const video = this.videoElement()?.nativeElement;
    if (video) {
      video.addEventListener('webkitendfullscreen', this.onIOSFullscreenEnd);
      video.addEventListener('webkitbeginfullscreen', this.onIOSFullscreenBegin);
    }
  }

  private removeFullscreenListeners(): void {
    document.removeEventListener('fullscreenchange', this.onFullscreenChange);
    document.removeEventListener('webkitfullscreenchange', this.onFullscreenChange);

    const video = this.videoElement()?.nativeElement;
    if (video) {
      video.removeEventListener('webkitendfullscreen', this.onIOSFullscreenEnd);
      video.removeEventListener('webkitbeginfullscreen', this.onIOSFullscreenBegin);
    }
  }

  private onFullscreenChange = (): void => {
    const isFs = !!document.fullscreenElement || !!(document as any).webkitFullscreenElement;
    this.isFullscreen.set(isFs);
  };

  private onIOSFullscreenEnd = (): void => {
    this.isFullscreen.set(false);
  };

  private onIOSFullscreenBegin = (): void => {
    this.isFullscreen.set(true);
  };

  private detectBunnyStream(): void {
    const url = this.url();
    if (!url) return;

    // Bunny Stream URL patterns:
    // - iframe.mediadelivery.net/embed/{library_id}/{video_id}
    // - video.bunnycdn.com/{library_id}/{video_id}
    // - {pull_zone}.b-cdn.net/{video_id}/playlist.m3u8

    if (url.includes('mediadelivery.net') || url.includes('bunnycdn.com')) {
      this.isBunnyStream.set(true);

      // If it's already an embed URL, use it directly
      if (url.includes('/embed/')) {
        this.bunnyEmbedUrl.set(this.sanitizer.bypassSecurityTrustResourceUrl(url));
      } else {
        // Convert to embed URL if possible
        const embedUrl = this.convertToBunnyEmbed(url);
        if (embedUrl) {
          this.bunnyEmbedUrl.set(this.sanitizer.bypassSecurityTrustResourceUrl(embedUrl));
        } else {
          // Fall back to HLS player
          this.isBunnyStream.set(false);
        }
      }
    } else if (url.includes('.b-cdn.net') && url.includes('playlist.m3u8')) {
      // This is a Bunny CDN HLS stream - use native player
      this.isBunnyStream.set(false);
    }
  }

  private convertToBunnyEmbed(url: string): string | null {
    // Try to extract library and video IDs from various URL formats
    const patterns = [
      /video\.bunnycdn\.com\/(\w+)\/(\w+)/,
      /(\w+)\.b-cdn\.net\/(\w+)\//
    ];

    for (const pattern of patterns) {
      const match = url.match(pattern);
      if (match) {
        const [, libraryId, videoId] = match;
        return `https://iframe.mediadelivery.net/embed/${libraryId}/${videoId}?autoplay=true&preload=true`;
      }
    }
    return null;
  }

  private initializePlayer(): void {
    const url = this.url();
    console.log('[VideoPlayer] initializePlayer called, url:', url, 'videoId:', this.videoId());
    const video = this.videoElement()?.nativeElement;
    if (!video || !url) {
      console.log('[VideoPlayer] initializePlayer: no video element or url');
      return;
    }

    // Set up video event listeners
    video.addEventListener('loadedmetadata', () => {
      this.duration.set(video.duration);
      this.isLoading.set(false);
      // Restore saved progress after metadata is loaded
      this.restoreProgress();
    });

    video.addEventListener('timeupdate', () => {
      this.currentTime.set(video.currentTime);
    });

    video.addEventListener('play', () => {
      this.isPlaying.set(true);
      // Clear resume hint once playing
      this.savedProgressTime.set(null);
    });
    video.addEventListener('pause', () => {
      this.isPlaying.set(false);
      // Save progress when paused
      this.saveProgress();
    });
    video.addEventListener('ended', () => {
      this.isPlaying.set(false);
      // Clear progress when video ends
      this.clearProgress();
    });
    video.addEventListener('waiting', () => this.isLoading.set(true));
    video.addEventListener('canplay', () => this.isLoading.set(false));
    video.addEventListener('error', () => {
      this.hasError.set(true);
      this.isLoading.set(false);
    });

    // Start periodic progress saving
    this.startProgressSaving();

    // Check if URL is HLS (.m3u8)
    const isHls = url.includes('.m3u8');

    if (isHls) {
      if (Hls.isSupported()) {
        this.hls = new Hls({
          enableWorker: true,
          lowLatencyMode: false,
        });
        this.hls.loadSource(url);
        this.hls.attachMedia(video);

        // Get quality levels when manifest is parsed
        this.hls.on(Hls.Events.MANIFEST_PARSED, (_, data) => {
          const qualities = data.levels.map((level, index) => ({
            height: level.height,
            label: level.height ? `${level.height}p` : `Niveau ${index + 1}`,
            index
          })).sort((a, b) => b.height - a.height);
          this.availableQualities.set(qualities);
          this.currentQuality.set(-1); // Auto by default
          video.play().catch(() => {});
        });

        // Track quality changes
        this.hls.on(Hls.Events.LEVEL_SWITCHED, (_, data) => {
          if (this.currentQuality() === -1) {
            // Auto mode - don't update UI
          }
        });

        this.hls.on(Hls.Events.ERROR, (_, data) => {
          if (data.fatal) {
            if (data.type === Hls.ErrorTypes.NETWORK_ERROR) {
              this.hls?.startLoad();
            } else if (data.type === Hls.ErrorTypes.MEDIA_ERROR) {
              this.hls?.recoverMediaError();
            } else {
              this.hasError.set(true);
            }
          }
        });
      } else if (video.canPlayType('application/vnd.apple.mpegurl')) {
        // Native HLS (Safari/iOS) - no quality selection available
        video.src = url;
        video.play().catch(() => {});
      } else {
        const mp4Url = url.replace('/playlist.m3u8', '/play_720p.mp4');
        video.src = mp4Url;
        video.play().catch(() => {});
      }
    } else {
      video.src = url;
      video.play().catch(() => {});
    }
  }

  private destroyPlayer(): void {
    if (this.hls) {
      this.hls.destroy();
      this.hls = null;
    }
  }

  private setupKeyboardControls(): void {
    document.addEventListener('keydown', this.handleKeydown);
  }

  private handleKeydown = (e: KeyboardEvent): void => {
    if (this.isBunnyStream()) return;

    const video = this.videoElement()?.nativeElement;
    if (!video) return;

    switch (e.key) {
      case ' ':
      case 'k':
        e.preventDefault();
        this.togglePlay();
        break;
      case 'ArrowLeft':
        e.preventDefault();
        this.skip(-10);
        break;
      case 'ArrowRight':
        e.preventDefault();
        this.skip(10);
        break;
      case 'm':
        e.preventDefault();
        this.toggleMute();
        break;
      case 'f':
        e.preventDefault();
        this.toggleFullscreen();
        break;
      case 'Escape':
        if (this.isFullscreen()) {
          this.toggleFullscreen();
        } else {
          // TODO: The 'emit' function requires a mandatory void argument
          this.close.emit();
        }
        break;
    }
  };

  // Player controls
  togglePlay(): void {
    const video = this.videoElement()?.nativeElement;
    if (!video) return;

    if (video.paused) {
      video.play();
    } else {
      video.pause();
    }
  }

  toggleMute(): void {
    const video = this.videoElement()?.nativeElement;
    if (!video) return;

    video.muted = !video.muted;
    this.isMuted.set(video.muted);
  }

  setVolume(event: Event): void {
    const video = this.videoElement()?.nativeElement;
    const input = event.target as HTMLInputElement;
    if (!video) return;

    const value = parseFloat(input.value);
    video.volume = value;
    this.volume.set(value);
    this.isMuted.set(value === 0);
  }

  seek(event: MouseEvent): void {
    const video = this.videoElement()?.nativeElement;
    const progressBar = event.currentTarget as HTMLElement;
    if (!video) return;

    const rect = progressBar.getBoundingClientRect();
    const pos = (event.clientX - rect.left) / rect.width;
    video.currentTime = pos * video.duration;
  }

  skip(seconds: number): void {
    const video = this.videoElement()?.nativeElement;
    if (!video) return;

    video.currentTime = Math.max(0, Math.min(video.currentTime + seconds, video.duration));
  }

  toggleFullscreen(): void {
    const video = this.videoElement()?.nativeElement as any;
    const container = this.playerContainer()?.nativeElement as any;

    // Check if we're currently in fullscreen
    const isCurrentlyFullscreen = this.isFullscreen() ||
      !!document.fullscreenElement ||
      !!(document as any).webkitFullscreenElement ||
      !!(document as any).mozFullScreenElement ||
      !!(document as any).msFullscreenElement;

    if (isCurrentlyFullscreen) {
      // Exit fullscreen
      if (document.exitFullscreen) {
        document.exitFullscreen();
      } else if ((document as any).webkitExitFullscreen) {
        (document as any).webkitExitFullscreen();
      } else if ((document as any).mozCancelFullScreen) {
        (document as any).mozCancelFullScreen();
      } else if ((document as any).msExitFullscreen) {
        (document as any).msExitFullscreen();
      } else if (video && video.webkitExitFullscreen) {
        video.webkitExitFullscreen();
      }
      this.isFullscreen.set(false);
      return;
    }

    // Enter fullscreen
    // Try container first (preferred for custom controls)
    if (container) {
      if (container.requestFullscreen) {
        container.requestFullscreen();
        this.isFullscreen.set(true);
        return;
      } else if (container.webkitRequestFullscreen) {
        container.webkitRequestFullscreen();
        this.isFullscreen.set(true);
        return;
      } else if (container.mozRequestFullScreen) {
        container.mozRequestFullScreen();
        this.isFullscreen.set(true);
        return;
      } else if (container.msRequestFullscreen) {
        container.msRequestFullscreen();
        this.isFullscreen.set(true);
        return;
      }
    }

    // Fallback: iOS Safari - use video element's native fullscreen
    if (video) {
      if (video.webkitEnterFullscreen) {
        video.webkitEnterFullscreen();
        this.isFullscreen.set(true);
        return;
      } else if (video.webkitRequestFullscreen) {
        video.webkitRequestFullscreen();
        this.isFullscreen.set(true);
        return;
      }
    }
  }

  // Quality selection
  toggleQualityMenu(): void {
    this.showQualityMenu.update(v => !v);
  }

  setQuality(index: number): void {
    if (this.hls) {
      this.hls.currentLevel = index;
      this.currentQuality.set(index);
    }
    this.showQualityMenu.set(false);
  }

  getQualityLabel(): string {
    const current = this.currentQuality();
    if (current === -1) return 'Auto';
    const quality = this.availableQualities().find(q => q.index === current);
    return quality ? quality.label : 'Auto';
  }

  onMouseMove(): void {
    this.showControls.set(true);
    if (this.controlsTimeout) {
      clearTimeout(this.controlsTimeout);
    }
    this.controlsTimeout = setTimeout(() => {
      if (this.isPlaying()) {
        this.showControls.set(false);
      }
    }, 3000);
  }

  download(): void {
    const url = this.downloadUrl() || this.url();
    if (url) {
      window.open(url, '_blank');
    }
  }

  formatTime(seconds: number): string {
    if (isNaN(seconds)) return '0:00';
    const mins = Math.floor(seconds / 60);
    const secs = Math.floor(seconds % 60);
    return `${mins}:${secs.toString().padStart(2, '0')}`;
  }

  // Progress saving methods
  private getStorageKey(): string | null {
    const videoId = this.videoId();
    if (!videoId) return null;
    return `${this.PROGRESS_STORAGE_KEY}${videoId}`;
  }

  private saveProgress(): void {
    const video = this.videoElement()?.nativeElement;
    if (!video || isNaN(video.currentTime) || isNaN(video.duration)) {
      console.log('[VideoPlayer] saveProgress: no video element or invalid time');
      return;
    }

    const currentTime = video.currentTime;
    const duration = video.duration;

    // Don't save if video just started
    if (currentTime < this.RESUME_THRESHOLD) {
      console.log('[VideoPlayer] saveProgress: skipping, currentTime < threshold', currentTime);
      return;
    }

    // Check if video is essentially complete
    const isComplete = duration > 0 && currentTime / duration >= this.COMPLETION_THRESHOLD;

    // Save to backend if authenticated
    const lessonId = this.getNumericLessonId();
    console.log('[VideoPlayer] saveProgress:', { lessonId, currentTime, duration, isComplete, isAuth: this.authService.isAuthenticated() });

    if (lessonId && this.authService.isAuthenticated()) {
      if (isComplete) {
        this.videoProgressService.saveProgress(lessonId, currentTime, duration, true).subscribe();
      } else {
        this.videoProgressService.saveProgress(lessonId, currentTime, duration, false).subscribe();
      }
    } else {
      console.log('[VideoPlayer] saveProgress: not saving to backend, lessonId:', lessonId, 'isAuth:', this.authService.isAuthenticated());
    }

    // Also save to localStorage as fallback
    const key = this.getStorageKey();
    if (!key) return;

    if (isComplete) {
      localStorage.removeItem(key);
      return;
    }

    const progressData = {
      time: currentTime,
      duration: duration,
      updatedAt: Date.now()
    };
    localStorage.setItem(key, JSON.stringify(progressData));
  }

  private restoreProgress(): void {
    const video = this.videoElement()?.nativeElement;
    if (!video) return;

    const lessonId = this.getNumericLessonId();

    // If authenticated, restore from backend
    if (lessonId && this.authService.isAuthenticated()) {
      this.videoProgressService.getProgress(lessonId).subscribe(progress => {
        if (progress.watchPosition && progress.watchPosition > this.RESUME_THRESHOLD && !progress.completed) {
          this.seekToPosition(video, progress.watchPosition);
        }
      });
      return;
    }

    // Fallback to localStorage
    const key = this.getStorageKey();
    if (!key) return;

    try {
      const savedData = localStorage.getItem(key);
      if (!savedData) return;

      const progressData = JSON.parse(savedData);
      const savedTime = progressData.time;

      if (savedTime && savedTime > this.RESUME_THRESHOLD) {
        this.seekToPosition(video, savedTime);
      }
    } catch (e) {
      localStorage.removeItem(key);
    }
  }

  private seekToPosition(video: HTMLVideoElement, position: number): void {
    const seekToSavedTime = () => {
      if (video.duration && position < video.duration * this.COMPLETION_THRESHOLD) {
        video.currentTime = position;
        this.currentTime.set(position);
      }
    };

    if (video.readyState >= 1) {
      seekToSavedTime();
    } else {
      video.addEventListener('loadedmetadata', seekToSavedTime, { once: true });
    }
  }

  private startProgressSaving(): void {
    const videoId = this.videoId();
    console.log('[VideoPlayer] startProgressSaving, videoId:', videoId);
    if (!videoId) {
      console.log('[VideoPlayer] No videoId, not starting progress saving');
      return;
    }

    // Save progress periodically
    this.saveProgressInterval = setInterval(() => {
      if (this.isPlaying()) {
        this.saveProgress();
      }
    }, this.SAVE_INTERVAL);
  }

  private clearProgress(): void {
    // Mark as completed on backend
    const lessonId = this.getNumericLessonId();
    if (lessonId && this.authService.isAuthenticated()) {
      const video = this.videoElement()?.nativeElement;
      const duration = video?.duration || 0;
      this.videoProgressService.saveProgress(lessonId, duration, duration, true).subscribe();
    }

    // Clear localStorage
    const key = this.getStorageKey();
    if (key) {
      localStorage.removeItem(key);
    }
  }
}
