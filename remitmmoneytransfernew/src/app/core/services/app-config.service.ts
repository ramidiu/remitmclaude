import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

export interface AppConfig {
  registrationEnabled: boolean;
  beneficiaryEnabled: boolean;
  transactionsEnabled: boolean;
  payinEnabled: boolean;
  payoutEnabled: boolean;
  liveRatesEnabled: boolean;
  maintenanceMode: boolean;
  maintenanceMessage: string;
  otpTtlMinutes: number;
  otpTtlSeconds: number;
}

const DEFAULTS: AppConfig = {
  registrationEnabled: true,
  beneficiaryEnabled: true,
  transactionsEnabled: true,
  payinEnabled: true,
  payoutEnabled: true,
  liveRatesEnabled: true,
  maintenanceMode: false,
  maintenanceMessage: '',
  otpTtlMinutes: 10,
  otpTtlSeconds: 600
};

/**
 * Loads the public app-config (GET /api/public/app-config) once at startup so the customer UI
 * reflects the operator's System Controls (feature toggles + OTP timing) instead of hardcoded
 * values. Falls back to safe defaults if the call fails, so the app never blocks on config.
 */
@Injectable({ providedIn: 'root' })
export class AppConfigService {
  private readonly config$ = new BehaviorSubject<AppConfig>(DEFAULTS);

  constructor(private http: HttpClient) {}

  /** Called by APP_INITIALIZER before the app renders. Never rejects. */
  load(): Promise<void> {
    return new Promise<void>((resolve) => {
      this.http.get<Partial<AppConfig>>(`${environment.apiUrl}/public/app-config`).pipe(
        catchError(() => of({} as Partial<AppConfig>))
      ).subscribe((cfg) => {
        this.config$.next({ ...DEFAULTS, ...(cfg || {}) });
        resolve();
      });
    });
  }

  /** Re-fetch on demand (e.g. after login or when returning to foreground). */
  refresh(): void { this.load(); }

  get config(): AppConfig { return this.config$.value; }
  get changes(): Observable<AppConfig> { return this.config$.asObservable(); }

  // Convenience getters for templates
  get registrationEnabled(): boolean { return this.config$.value.registrationEnabled; }
  get beneficiaryEnabled(): boolean { return this.config$.value.beneficiaryEnabled; }
  get transactionsEnabled(): boolean { return this.config$.value.transactionsEnabled; }
  get liveRatesEnabled(): boolean { return this.config$.value.liveRatesEnabled; }
  get otpTtlSeconds(): number { return this.config$.value.otpTtlSeconds; }
  get maintenanceMode(): boolean { return this.config$.value.maintenanceMode; }
  get maintenanceMessage(): string { return this.config$.value.maintenanceMessage; }
}
