import { Component } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';

/**
 * PUBLIC, no-login account deletion page (Google Play Account Deletion policy).
 * Two steps, ownership proven by an email OTP:
 *   1) enter email  -> backend emails a 6-digit code
 *   2) enter code (+ optional reason) -> account is deactivated (soft delete)
 * Reachable at /account-deletion without authentication.
 */
@Component({
  selector: 'app-account-deletion',
  template: `
  <ion-content><div class="info-page">
    <div class="info-hero">
      <a class="info-back" href="/">&larr; Back to Home</a>
      <h1>Delete Your Account</h1>
      <p>Request deletion of your Remitm Money Transfer account</p>
    </div>

    <div class="info-body">
      <!-- STEP 1: email -->
      <div *ngIf="step === 'email'">
        <p>Enter the email address associated with your Remitm account. We'll send a
           6-digit verification code to confirm it's you.</p>
        <label class="ad-label">Email address</label>
        <input class="ad-input" type="email" inputmode="email" autocomplete="email"
               placeholder="you@example.com" [(ngModel)]="email" [disabled]="loading" />
        <p *ngIf="error" class="ad-error">{{ error }}</p>
        <button class="ad-btn" (click)="sendCode()" [disabled]="loading || !email">
          {{ loading ? 'Sending…' : 'Send verification code' }}
        </button>
      </div>

      <!-- STEP 2: otp + reason + confirm -->
      <div *ngIf="step === 'otp'">
        <p>We sent a 6-digit code to <strong>{{ email }}</strong>. Enter it below to confirm deletion.</p>
        <label class="ad-label">Verification code</label>
        <input class="ad-input" type="text" inputmode="numeric" maxlength="6"
               placeholder="123456" [(ngModel)]="otp" [disabled]="loading" />
        <label class="ad-label">Reason (optional)</label>
        <textarea class="ad-input" rows="3" placeholder="Tell us why you're leaving (optional)"
                  [(ngModel)]="reason" [disabled]="loading"></textarea>
        <label class="ad-check">
          <input type="checkbox" [(ngModel)]="confirmed" [disabled]="loading" />
          <span>I understand my account will be deactivated and this cannot be undone.</span>
        </label>
        <p *ngIf="error" class="ad-error">{{ error }}</p>
        <button class="ad-btn ad-btn--danger" (click)="confirmDelete()" [disabled]="loading || !otp || !confirmed">
          {{ loading ? 'Deleting…' : 'Delete my account' }}
        </button>
        <button class="ad-link" (click)="step='email'; error=''" [disabled]="loading">Use a different email</button>
      </div>

      <!-- STEP 3: done -->
      <div *ngIf="step === 'done'" class="ad-success">
        <h2>Request received</h2>
        <p>Your account deletion request has been confirmed and your account deactivated.
           A confirmation email has been sent to <strong>{{ email }}</strong>.</p>
        <a class="ad-btn" href="/">Back to Home</a>
      </div>

      <!-- legal / info -->
      <h2>What Happens After Verification</h2>
      <ul>
        <li>Account access will be removed.</li>
        <li>Profile information and saved recipients will be deleted.</li>
        <li>Marketing preferences will be deleted.</li>
      </ul>
      <h2>Data Retained for Legal Reasons</h2>
      <p>Due to financial regulations, AML/KYC requirements, fraud prevention, and tax obligations,
         certain transaction history and identity verification records may be retained for the legally
         required retention period before permanent deletion.</p>
      <h2>Processing Time</h2>
      <p>Deletion requests are normally processed within 30 days.</p>
      <h2>Need Help?</h2>
      <p>For assistance contact <a href="mailto:support@remitm.com">support@remitm.com</a>.
         See also our <a href="/privacy-policy">Privacy Policy</a>.</p>
    </div>
  </div></ion-content>`,
  styles: [`
    .ad-label{display:block;font-weight:600;margin:14px 0 6px;color:#1f2937;font-size:14px;}
    .ad-input{width:100%;box-sizing:border-box;padding:11px 14px;border:1px solid #d1d5db;border-radius:8px;font-size:15px;}
    .ad-input:focus{outline:none;border-color:#003377;box-shadow:0 0 0 3px rgba(0,51,119,.08);}
    .ad-check{display:flex;gap:10px;align-items:flex-start;margin:16px 0;font-size:14px;color:#374151;}
    .ad-check input{margin-top:3px;}
    .ad-btn{display:inline-block;width:100%;text-align:center;margin-top:18px;padding:13px;border:none;border-radius:8px;
      background:#003377;color:#fff;font-size:15px;font-weight:600;cursor:pointer;text-decoration:none;}
    .ad-btn:disabled{opacity:.5;cursor:not-allowed;}
    .ad-btn--danger{background:#dc2626;}
    .ad-link{display:block;width:100%;margin-top:10px;background:none;border:none;color:#003377;font-size:14px;cursor:pointer;}
    .ad-error{color:#dc2626;font-size:14px;margin-top:10px;}
    .ad-success h2{margin-top:0;color:#15803d;}
  `]
})
export class AccountDeletionPage {
  step: 'email' | 'otp' | 'done' = 'email';
  email = '';
  otp = '';
  reason = '';
  confirmed = false;
  loading = false;
  error = '';

  private api = environment.apiUrl;

  constructor(private http: HttpClient) {}

  sendCode(): void {
    this.error = '';
    const email = (this.email || '').trim();
    if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(email)) { this.error = 'Please enter a valid email address.'; return; }
    this.loading = true;
    this.http.post(`${this.api}/account/public/delete-request`, { email }).subscribe({
      next: () => { this.loading = false; this.step = 'otp'; this.otp = ''; },
      error: () => { this.loading = false; this.step = 'otp'; this.otp = ''; } // generic: don't reveal existence
    });
  }

  confirmDelete(): void {
    this.error = '';
    if (!this.otp || !this.confirmed) { return; }
    this.loading = true;
    this.http.post(`${this.api}/account/public/delete-confirm`,
      { email: this.email.trim(), otp: this.otp.trim(), reason: (this.reason || '').trim() || undefined })
      .subscribe({
        next: () => { this.loading = false; this.step = 'done'; },
        error: (err) => {
          this.loading = false;
          this.error = err?.error?.message || 'Invalid or expired code. Please check and try again.';
        }
      });
  }
}
