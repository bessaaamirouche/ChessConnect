import { Component, ChangeDetectionStrategy } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'app-footer',
  standalone: true,
  imports: [RouterLink, TranslateModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <footer class="footer">
      <div class="footer__container">
        <div class="footer__main">
          <div class="footer__brand">
            <img src="assets/logo.webp" alt="myChess" class="footer__logo" loading="lazy">
          </div>

          <div class="footer__columns">
            <div class="footer__column">
              <h4>{{ 'footer.company' | translate }}</h4>
              <a routerLink="/coaches">{{ 'teachers.title' | translate }}</a>
              <a routerLink="/pricing">{{ 'nav.pricing' | translate }}</a>
              <a routerLink="/register">{{ 'nav.signup' | translate }}</a>
              <a routerLink="/login">{{ 'nav.login' | translate }}</a>
            </div>

            <div class="footer__column">
              <h4>{{ 'footer.support' | translate }}</h4>
              <a routerLink="/blog">{{ 'nav.blog' | translate }}</a>
              <a routerLink="/how-it-works">{{ 'nav.howItWorks' | translate }}</a>
              <a routerLink="/faq">{{ 'nav.faq' | translate }}</a>
            </div>

            <div class="footer__column">
              <h4>{{ 'footer.social' | translate }}</h4>
              <a href="mailto:support@mychess.fr">support&#64;mychess.fr</a>
              <a routerLink="/links">{{ 'footer.ourNetworks' | translate }}</a>
              <img src="assets/qr-links.png" alt="QR Code mychess" class="footer__qr-code" loading="lazy">
            </div>
          </div>
        </div>

        <div class="footer__divider"></div>

        <div class="footer__bottom">
          <p class="footer__copyright">&copy; 2026 mychess. {{ 'footer.copyright' | translate }}</p>
          <div class="footer__legal">
            <a routerLink="/privacy">{{ 'footer.privacy' | translate }}</a>
            <a routerLink="/terms">{{ 'footer.terms' | translate }}</a>
            <a routerLink="/legal-notice">{{ 'footer.legal' | translate }}</a>
          </div>
        </div>
      </div>
    </footer>
  `,
  styleUrl: './footer.component.scss'
})
export class FooterComponent {}
