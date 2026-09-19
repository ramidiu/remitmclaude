import { Component, OnInit } from '@angular/core';
import { Router, NavigationEnd } from '@angular/router';
import { filter, map, startWith } from 'rxjs/operators';
import { combineLatest, Observable } from 'rxjs';
import { AppConfigService } from './core/services/app-config.service';

// Code added by Naresh: Maintenance Mode — full-screen customer block.
// When the superadmin toggles `maintenance.mode.enabled` ON (System Controls),
// the whole customer app is replaced by a "temporarily unavailable" screen so no
// page and no link is reachable. Operator areas (admin/superadmin/agent/partner)
// are intentionally spared so staff can still log in and switch maintenance off.
@Component({
  selector: 'app-root',
  template: `
    <ion-app>
      <!-- ===== MAINTENANCE FULL-SCREEN BLOCK ===== -->
      <div *ngIf="showMaintenance$ | async" class="maintenance-screen">
        <div class="maintenance-card">
          <img src="assets/images/remitm-logo.svg" alt="Remitm" class="maintenance-logo" />
          <div class="maintenance-icon">
            <ion-icon name="construct-outline"></ion-icon>
          </div>
          <h1 class="maintenance-title">Temporary Suspension of Payment Service</h1>
          <p class="maintenance-message">{{ maintenanceMessage }}</p>
          <div class="maintenance-contact">
            <a class="maintenance-contact__item" href="mailto:info@remitm.com">
              <ion-icon name="mail-outline"></ion-icon> info&#64;remitm.com
            </a>
            <a class="maintenance-contact__item" href="tel:+442085560888">
              <ion-icon name="call-outline"></ion-icon> 020 8556 0888
            </a>
          </div>
          <p class="maintenance-note">We apologise for the inconvenience.</p>
        </div>
      </div>

      <!-- Router outlet is only rendered when NOT blocked -->
      <ion-router-outlet *ngIf="!(showMaintenance$ | async)"></ion-router-outlet>
    </ion-app>
  `,
  styles: [`
    .maintenance-screen {
      position: fixed;
      inset: 0;
      z-index: 100000;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 24px;
      background: linear-gradient(160deg, #003377 0%, #001a3d 100%);
      overflow: auto;
    }
    .maintenance-card {
      max-width: 440px;
      width: 100%;
      text-align: center;
      background: #ffffff;
      border-radius: 20px;
      padding: 40px 28px;
      box-shadow: 0 24px 60px rgba(0,0,0,0.35);
    }
    .maintenance-logo { width: 150px; height: auto; margin-bottom: 24px; }
    .maintenance-icon {
      width: 72px; height: 72px; margin: 0 auto 20px;
      display: flex; align-items: center; justify-content: center;
      border-radius: 50%;
      background: rgba(93,187,82,0.12);
      color: #5DBB52;
    }
    .maintenance-icon ion-icon { font-size: 38px; }
    .maintenance-title { font-size: 22px; font-weight: 700; color: #003377; margin: 0 0 12px; }
    .maintenance-message { font-size: 15px; line-height: 1.6; color: #333; margin: 0 0 16px; white-space: pre-line; }
    .maintenance-contact {
      display: flex; flex-direction: column; align-items: center; gap: 8px;
      margin: 0 0 16px;
    }
    .maintenance-contact__item {
      display: inline-flex; align-items: center; gap: 8px;
      font-size: 14px; font-weight: 600; color: #003377; text-decoration: none;
    }
    .maintenance-contact__item ion-icon { font-size: 16px; color: #5DBB52; }
    .maintenance-contact__item:hover { text-decoration: underline; }
    .maintenance-note { font-size: 13px; color: #888; margin: 0; }
  `]
})
export class AppComponent implements OnInit {
  showMaintenance$!: Observable<boolean>;

  // Route prefixes that bypass the block so operators can always reach the
  // controls to turn maintenance mode off again.
  private readonly operatorPrefixes = [
    '/admin', '/admin-login', '/admin-mfa-setup', '/superadmin',
    '/agent', '/partner', '/payin-partner'
  ];

  constructor(private router: Router, private appConfig: AppConfigService) {}

  get maintenanceMessage(): string {
    return this.appConfig.maintenanceMessage
      || 'The system is currently undergoing scheduled maintenance.';
  }

  ngOnInit(): void {
    const url$ = this.router.events.pipe(
      filter((e): e is NavigationEnd => e instanceof NavigationEnd),
      map((e) => e.urlAfterRedirects || e.url),
      startWith(this.router.url)
    );

    this.showMaintenance$ = combineLatest([this.appConfig.changes, url$]).pipe(
      map(([cfg, url]) => cfg.maintenanceMode && !this.isOperatorRoute(url))
    );
  }

  private isOperatorRoute(url: string): boolean {
    const path = (url || '').split('?')[0].split('#')[0];
    return this.operatorPrefixes.some(p => path === p || path.startsWith(p + '/'));
  }
}
