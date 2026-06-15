import { Component } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { ToastController } from '@ionic/angular';
import { environment } from '../../../environments/environment';

/**
 * Admin tool to manually trigger an Nsano or Zeepay payout for a transaction,
 * and to check its status. Mirrors the admin-triggered payout flow of the
 * original RemitM ops console. Endpoints:
 *   POST /api/payout/nsano/initiate | /status
 *   POST /api/payout/zeepay/initiate | /status
 */
@Component({
  selector: 'app-admin-payouts',
  templateUrl: './admin-payouts.page.html',
  styleUrls: ['./admin-payouts.page.scss']
})
export class AdminPayoutsPage {
  private readonly api = environment.apiUrl;

  provider: 'nsano' | 'zeepay' = 'nsano';
  serviceType: 'WALLET' | 'BANK' | 'PICKUP' = 'WALLET';

  referenceNumber = '';

  // Recipient (shared form fields, mapped per provider on submit)
  recipientName = '';
  receiverFirstName = '';
  receiverLastName = '';
  receiverCountry = 'GH';
  address = '';
  narration = '';

  // Mobile money
  dialingCode = '233';
  mobileNumber = '';
  network = 'MTN';        // mno / destinationHouse for wallet

  // Bank
  accountNumber = '';
  bankCode = '';          // destinationHouse (SWIFT) for Nsano bank
  routingNumber = '';

  submitting = false;
  checking = false;
  response: any = null;
  statusResult: any = null;

  constructor(private http: HttpClient, private toast: ToastController) {}

  onProviderChange(): void {
    // Pickup only exists for Zeepay; reset if switching to Nsano
    if (this.provider === 'nsano' && this.serviceType === 'PICKUP') {
      this.serviceType = 'WALLET';
    }
  }

  get isWallet(): boolean { return this.serviceType === 'WALLET'; }
  get isBank(): boolean { return this.serviceType === 'BANK'; }
  get isPickup(): boolean { return this.serviceType === 'PICKUP'; }

  initiate(): void {
    if (!this.referenceNumber.trim()) {
      this.notify('Enter the transaction reference number.', 'warning');
      return;
    }
    this.submitting = true;
    this.response = null;

    let url: string;
    let body: any;

    if (this.provider === 'nsano') {
      url = `${this.api}/payout/nsano/initiate`;
      body = {
        referenceNumber: this.referenceNumber.trim(),
        paymentType: this.serviceType,                       // WALLET | BANK
        destinationHouse: this.isWallet ? this.network : this.bankCode,
        recipient: this.isWallet ? this.mobileNumber : this.accountNumber,
        recipientName: this.recipientName,
        narration: this.narration
      };
    } else {
      url = `${this.api}/payout/zeepay/initiate`;
      body = {
        referenceNumber: this.referenceNumber.trim(),
        serviceType: this.serviceType,                       // WALLET | BANK | PICKUP
        receiverFirstName: this.receiverFirstName,
        receiverLastName: this.receiverLastName,
        receiverCountry: this.receiverCountry,
        address: this.address,
        dialingCode: this.dialingCode,
        recipientMsisdn: this.mobileNumber,
        mno: this.network,
        accountNumber: this.accountNumber,
        routingNumber: this.routingNumber
      };
    }

    this.http.post(url, body).subscribe({
      next: (res) => {
        this.submitting = false;
        this.response = res;
        this.notify('Payout initiated. Check the response below.', 'success');
      },
      error: (err) => {
        this.submitting = false;
        this.response = err?.error || { error: 'Request failed' };
        this.notify(err?.error?.message || err?.error?.error || 'Payout failed.', 'danger');
      }
    });
  }

  checkStatus(): void {
    if (!this.referenceNumber.trim()) {
      this.notify('Enter the transaction reference number.', 'warning');
      return;
    }
    this.checking = true;
    this.statusResult = null;
    const url = `${this.api}/payout/${this.provider}/status`;
    this.http.post(url, { referenceNumber: this.referenceNumber.trim() }).subscribe({
      next: (res) => { this.checking = false; this.statusResult = res; },
      error: (err) => { this.checking = false; this.statusResult = err?.error || { error: 'Status check failed' }; }
    });
  }

  pretty(obj: any): string {
    try { return JSON.stringify(obj, null, 2); } catch { return String(obj); }
  }

  private async notify(message: string, color: string): Promise<void> {
    const t = await this.toast.create({ message, color, duration: 2600, position: 'top' });
    await t.present();
  }
}
