import { Injectable, signal } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';

export type Language = 'fr' | 'en';

@Injectable({
  providedIn: 'root'
})
export class LanguageService {
  private readonly STORAGE_KEY = 'mychess_language';

  currentLang = signal<Language>('fr');

  readonly availableLanguages: { code: Language; name: string; flag: string }[] = [
    { code: 'fr', name: 'Français', flag: '🇫🇷' },
    { code: 'en', name: 'English', flag: '🇬🇧' }
  ];

  constructor(private translate: TranslateService) {
    this.initLanguage();
  }

  private initLanguage(): void {
    const savedLang = localStorage.getItem(this.STORAGE_KEY) as Language | null;
    // Toujours FR par défaut, sauf si l'utilisateur a explicitement choisi une autre langue
    const defaultLang: Language = savedLang || 'fr';

    this.translate.setDefaultLang('fr');
    this.translate.addLangs(['fr', 'en']);
    this.setLanguage(defaultLang);
  }

  setLanguage(lang: Language): void {
    this.translate.use(lang);
    this.currentLang.set(lang);
    localStorage.setItem(this.STORAGE_KEY, lang);
    document.documentElement.lang = lang;
  }

  toggleLanguage(): void {
    const newLang: Language = this.currentLang() === 'fr' ? 'en' : 'fr';
    this.setLanguage(newLang);
  }

  getCurrentLanguage(): Language {
    return this.currentLang();
  }
}
