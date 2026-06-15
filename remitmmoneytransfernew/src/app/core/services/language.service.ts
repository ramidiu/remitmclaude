import { Injectable } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';

export type SupportedLanguage = 'en';

@Injectable({ providedIn: 'root' })
export class LanguageService {
  private readonly currentLang: SupportedLanguage = 'en';

  constructor(private translate: TranslateService) {}

  init(): void {
    this.translate.addLangs(['en']);
    this.translate.setDefaultLang('en');
    this.setLanguage('en');
  }

  setLanguage(_lang: SupportedLanguage = 'en'): void {
    this.translate.use('en');
    const html = document.getElementById('html-root') || document.documentElement;
    html.setAttribute('lang', 'en');
    html.setAttribute('dir', 'ltr');
  }
}
