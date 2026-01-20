import {
  Directive,
  ElementRef,
  Input,
  OnInit,
  OnDestroy,
  inject,
  PLATFORM_ID
} from '@angular/core';
import { isPlatformBrowser } from '@angular/common';

export type RevealAnimation =
  | 'fade-up'
  | 'fade-down'
  | 'fade-left'
  | 'fade-right'
  | 'scale'
  | 'scale-up';

@Directive({
  selector: '[appScrollReveal]',
  standalone: true
})
export class ScrollRevealDirective implements OnInit, OnDestroy {
  private el = inject(ElementRef);
  private platformId = inject(PLATFORM_ID);
  private observer: IntersectionObserver | null = null;

  @Input('appScrollReveal') animation: RevealAnimation = 'fade-up';
  @Input() revealDelay = 0;
  @Input() revealThreshold = 0.15;
  @Input() revealOnce = false; // Changed to false for reverse animations by default

  ngOnInit(): void {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }

    this.setupElement();
    this.createObserver();
  }

  ngOnDestroy(): void {
    this.observer?.disconnect();
  }

  private setupElement(): void {
    const element = this.el.nativeElement as HTMLElement;

    // Map animation type to CSS class
    const classMap: Record<RevealAnimation, string> = {
      'fade-up': 'reveal-up',
      'fade-down': 'reveal-down',
      'fade-left': 'reveal-left',
      'fade-right': 'reveal-right',
      'scale': 'reveal-scale',
      'scale-up': 'reveal-scale-up'
    };

    element.classList.add(classMap[this.animation] || 'reveal-up');

    // Apply delay if specified
    if (this.revealDelay > 0) {
      element.style.transitionDelay = `${this.revealDelay}ms`;
    }
  }

  private createObserver(): void {
    const options: IntersectionObserverInit = {
      root: null,
      rootMargin: '50px 0px -100px 0px', // More margin for smoother reveal/hide
      threshold: this.revealThreshold
    };

    this.observer = new IntersectionObserver((entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          entry.target.classList.add('reveal--visible');

          if (this.revealOnce) {
            this.observer?.unobserve(entry.target);
          }
        } else if (!this.revealOnce) {
          // Reset the delay to 0 when hiding for immediate effect
          (entry.target as HTMLElement).style.transitionDelay = '0ms';
          entry.target.classList.remove('reveal--visible');
          // Restore delay after hiding
          setTimeout(() => {
            if (this.revealDelay > 0) {
              (entry.target as HTMLElement).style.transitionDelay = `${this.revealDelay}ms`;
            }
          }, 50);
        }
      });
    }, options);

    this.observer.observe(this.el.nativeElement);
  }
}

/**
 * Directive for stagger animations on child elements
 */
@Directive({
  selector: '[appStaggerReveal]',
  standalone: true
})
export class StaggerRevealDirective implements OnInit, OnDestroy {
  private el = inject(ElementRef);
  private platformId = inject(PLATFORM_ID);
  private observer: IntersectionObserver | null = null;

  @Input() staggerThreshold = 0.1;
  @Input() staggerOnce = false; // Changed to false for reverse animations

  ngOnInit(): void {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }

    const element = this.el.nativeElement as HTMLElement;
    element.classList.add('stagger-children');

    this.createObserver();
  }

  ngOnDestroy(): void {
    this.observer?.disconnect();
  }

  private createObserver(): void {
    const options: IntersectionObserverInit = {
      root: null,
      rootMargin: '50px 0px -100px 0px',
      threshold: this.staggerThreshold
    };

    this.observer = new IntersectionObserver((entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          entry.target.classList.add('stagger--visible');

          if (this.staggerOnce) {
            this.observer?.unobserve(entry.target);
          }
        } else if (!this.staggerOnce) {
          entry.target.classList.remove('stagger--visible');
        }
      });
    }, options);

    this.observer.observe(this.el.nativeElement);
  }
}

/**
 * Directive for parallax scrolling effect
 */
@Directive({
  selector: '[appParallax]',
  standalone: true
})
export class ParallaxDirective implements OnInit, OnDestroy {
  private el = inject(ElementRef);
  private platformId = inject(PLATFORM_ID);
  private scrollListener: (() => void) | null = null;
  private rafId: number | null = null;

  @Input() parallaxSpeed = 0.5;
  @Input() parallaxDirection: 'up' | 'down' = 'up';

  ngOnInit(): void {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }

    const element = this.el.nativeElement as HTMLElement;
    element.classList.add('parallax');

    this.scrollListener = () => {
      if (this.rafId) {
        cancelAnimationFrame(this.rafId);
      }
      this.rafId = requestAnimationFrame(() => this.updatePosition());
    };

    window.addEventListener('scroll', this.scrollListener, { passive: true });
    this.updatePosition();
  }

  ngOnDestroy(): void {
    if (this.scrollListener) {
      window.removeEventListener('scroll', this.scrollListener);
    }
    if (this.rafId) {
      cancelAnimationFrame(this.rafId);
    }
  }

  private updatePosition(): void {
    const element = this.el.nativeElement as HTMLElement;
    const rect = element.getBoundingClientRect();
    const windowHeight = window.innerHeight;

    // Only animate when element is in view
    if (rect.bottom < 0 || rect.top > windowHeight) {
      return;
    }

    const scrolled = window.scrollY;
    const elementTop = rect.top + scrolled;
    const relativeScroll = scrolled - elementTop + windowHeight;
    const movement = relativeScroll * this.parallaxSpeed * 0.1;

    const direction = this.parallaxDirection === 'up' ? -1 : 1;
    element.style.transform = `translateY(${movement * direction}px)`;
  }
}
