import { Component, OnInit } from '@angular/core';
import { ToastController, AlertController } from '@ionic/angular';
import { PartnerService } from '../../core/services/partner.service';

@Component({
  selector: 'app-partner-transactions',
  template: `
    <div class="partner-transactions">
      <div class="pt-header">
        <div>
          <h1 class="page-title">Pending Payouts</h1>
          <p class="page-subtitle">Transactions awaiting payout confirmation</p>
        </div>
        <ion-button fill="outline" size="small" (click)="loadAll()">
          <ion-icon name="refresh-outline" slot="start"></ion-icon>
          Refresh
        </ion-button>
      </div>

      <!-- Tabs -->
      <div class="pt-tabs">
        <button class="pt-tab" [class.pt-tab--active]="activeTab === 'payout'" (click)="activeTab = 'payout'">
          <ion-icon name="arrow-redo-outline"></ion-icon>
          Payout Transactions
          <span class="pt-tab-count">{{ payoutTxns.length }}</span>
        </button>
        <button class="pt-tab" [class.pt-tab--active]="activeTab === 'payin'" (click)="activeTab = 'payin'">
          <ion-icon name="arrow-undo-outline"></ion-icon>
          Pay-In Transactions
          <span class="pt-tab-count pt-tab-count--orange">{{ payinTxns.length }}</span>
        </button>
      </div>

      <!-- ===== PAYOUT TAB ===== -->
      <div *ngIf="activeTab === 'payout'" class="fb-card table-card">
        <div *ngIf="loadingPayout" class="loading">
          <div class="fb-skeleton fb-skeleton--text" *ngFor="let i of [1,2,3,4,5]"></div>
        </div>
        <div class="table-wrapper" *ngIf="!loadingPayout">
          <table class="fb-table">
            <thead>
              <tr>
                <th>Reference</th>
                <th>Beneficiary</th>
                <th>Payout Amount</th>
                <th>Delivery Method</th>
                <th>Status</th>
                <th>Created</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let txn of payoutTxns">
                <td class="fb-currency">{{ txn.referenceNumber }}</td>
                <td>{{ txn.beneficiaryName }}</td>
                <td class="fb-currency">{{ txn.receiveAmount | number:'1.2-2' }} {{ txn.receiveCurrency }}</td>
                <td>{{ txn.deliveryMethod }}</td>
                <td><app-status-chip [status]="txn.status"></app-status-chip></td>
                <td>{{ txn.createdAt | date:'MMM d, y HH:mm' }}</td>
                <td>
                  <div class="actions">
                    <button class="action-btn action-btn--icon action-btn--info" (click)="viewBeneficiary(txn)" title="View beneficiary details">
                      <ion-icon name="eye-outline"></ion-icon>
                    </button>
                    <!-- "Paid" is ONLY for MANUAL payouts (a human pays out-of-band and confirms).
                         Nsano/Zeepay are automatic — PAID comes from the provider (auto-disburse →
                         callback/poll, or Check Status on the Gateway Ops page), never a button. -->
                    <button *ngIf="txn.payoutGateway === 'MANUAL'" class="action-btn action-btn--success" (click)="markPayoutPaid(txn)" [disabled]="txn._marking" title="Mark as paid (manual payout)">
                      <ion-icon name="checkmark-circle-outline"></ion-icon>
                      {{ txn._marking ? '…' : 'Paid' }}
                    </button>
                    <span *ngIf="txn.payoutGateway && txn.payoutGateway !== 'MANUAL'" class="auto-note" title="Automatic gateway — settles via the provider">
                      <ion-icon name="flash-outline"></ion-icon> Auto ({{ txn.payoutGateway }})
                    </span>
                  </div>
                </td>
              </tr>
              <tr *ngIf="payoutTxns.length === 0">
                <td colspan="7" class="empty-state">
                  <ion-icon name="checkmark-circle-outline" class="empty-icon"></ion-icon>
                  <p>All caught up! No pending payouts.</p>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- ===== PAY-IN TAB ===== -->
      <div *ngIf="activeTab === 'payin'" class="fb-card table-card">
        <div class="pt-payin-info">
          <ion-icon name="information-circle-outline"></ion-icon>
          These are Pay-In transactions with status <strong>PROCESSING</strong> awaiting payout. Mark them as Paid once funds are disbursed.
        </div>
        <div *ngIf="loadingPayin" class="loading">
          <div class="fb-skeleton fb-skeleton--text" *ngFor="let i of [1,2,3,4,5]"></div>
        </div>
        <div class="table-wrapper" *ngIf="!loadingPayin">
          <table class="fb-table">
            <thead>
              <tr>
                <th>Transaction ID</th>
                <th>Customer</th>
                <th>Source</th>
                <th>Beneficiary</th>
                <th>Bank</th>
                <th>Amount</th>
                <th>Receives</th>
                <th>Delivery</th>
                <th>Created</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let txn of payinTxns">
                <td class="mono" [title]="txn.referenceNumber || txn.transactionId">{{ txn.referenceNumber || (txn.transactionId | slice:0:8) + '…' }}</td>
                <td class="mono" [title]="txn.customerId">{{ txn.customerId | slice:0:8 }}…</td>
                <td>
                  <span class="src-badge" [class.src-badge--backend]="txn.customerSource === 'BACKEND'"
                        [class.src-badge--frontend]="txn.customerSource === 'FRONTEND'"
                        [class.src-badge--user]="txn.customerSource === 'FRONTEND_USER'">
                    {{ txn.customerSource === 'FRONTEND_USER' ? 'UK USER' : txn.customerSource }}
                  </span>
                </td>
                <td>{{ txn.beneficiaryName || '—' }}</td>
                <td>{{ txn.beneficiaryBank || '—' }}</td>
                <td class="fb-currency">{{ txn.amount | number:'1.2-2' }} {{ txn.currency }}</td>
                <td class="fb-currency">
                  <span *ngIf="txn.receiveAmount">{{ txn.receiveAmount | number:'1.2-2' }} {{ txn.receiveCurrency }}</span>
                  <span *ngIf="!txn.receiveAmount" class="muted">—</span>
                </td>
                <td>{{ txn.deliveryMethod || '—' }}</td>
                <td>{{ txn.createdAt | date:'MMM d, y HH:mm' }}</td>
                <td>
                  <div class="actions">
                    <button class="action-btn action-btn--icon action-btn--info" (click)="viewBeneficiary(txn)" title="View beneficiary details">
                      <ion-icon name="eye-outline"></ion-icon>
                    </button>
                    <button class="action-btn action-btn--success" (click)="markPayinPaid(txn)" [disabled]="txn._marking" title="Mark as paid">
                      <ion-icon name="checkmark-circle-outline"></ion-icon>
                      {{ txn._marking ? '…' : 'Paid' }}
                    </button>
                  </div>
                </td>
              </tr>
              <tr *ngIf="payinTxns.length === 0">
                <td colspan="10" class="empty-state">
                  <ion-icon name="checkmark-circle-outline" class="empty-icon"></ion-icon>
                  <p>No Pay-In transactions awaiting payout.</p>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .partner-transactions { padding: 20px 24px; }
    .pt-header { display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: 14px; }
    .page-title { font-size: 1.35rem; font-weight: 700; margin: 0 0 2px; }
    .page-subtitle { color: #6b7280; margin: 0; font-size: 0.8rem; }

    /* Tighter, professional table (scoped to this page only) */
    .table-card { border-radius: 10px; box-shadow: 0 1px 2px rgba(16,24,40,.06); }
    .partner-transactions .fb-table { width: 100%; border-collapse: collapse; font-size: 0.82rem; }
    .partner-transactions .fb-table th {
      padding: 9px 14px; font-size: 0.68rem; font-weight: 600; letter-spacing: .04em;
      text-transform: uppercase; color: #6b7280; background: #f9fafb; border-bottom: 1px solid #e5e7eb;
      white-space: nowrap; text-align: left;
    }
    .partner-transactions .fb-table td { padding: 9px 14px; border-bottom: 1px solid #f1f3f5; vertical-align: middle; }
    .partner-transactions .fb-table tbody tr:hover { background: #fafbfc; }
    .partner-transactions .fb-table tbody tr:last-child td { border-bottom: none; }

    .pt-tabs { display: flex; gap: 4px; margin-bottom: 16px; border-bottom: 2px solid #e5e7eb; }
    .pt-tab { display: flex; align-items: center; gap: 8px; padding: 10px 20px; background: none; border: none; border-bottom: 3px solid transparent; margin-bottom: -2px; cursor: pointer; font-size: 0.9rem; font-weight: 500; color: #6b7280; transition: all .15s; }
    .pt-tab:hover { color: #003377; }
    .pt-tab--active { color: #003377; border-bottom-color: #003377; }
    .pt-tab-count { background: #e5e7eb; color: #374151; border-radius: 12px; padding: 1px 8px; font-size: 0.75rem; font-weight: 700; }
    .pt-tab-count--orange { background: #fef3c7; color: #92400e; }

    .pt-payin-info { display: flex; align-items: flex-start; gap: 8px; padding: 10px 16px; background: #eff6ff; border-bottom: 1px solid #bfdbfe; font-size: 0.85rem; color: #1e40af; }
    .pt-payin-info ion-icon { font-size: 1.1rem; flex-shrink: 0; margin-top: 1px; }

    .table-wrapper { overflow-x: auto; }
    .loading { padding: 16px; display: flex; flex-direction: column; gap: 8px; }
    .empty-state { text-align: center; color: #9ca3af; padding: 32px !important; }
    .empty-icon { font-size: 2rem; display: block; margin: 0 auto 8px; }
    .mono { font-family: monospace; font-size: 0.8rem; cursor: default; }
    .muted { color: #9ca3af; }
    .fb-currency { font-weight: 600; }

    .src-badge { display: inline-block; padding: 2px 8px; border-radius: 10px; font-size: 0.7rem; font-weight: 700; text-transform: uppercase; }
    .src-badge--backend { background: #dbeafe; color: #1e40af; }
    .src-badge--frontend { background: #d1fae5; color: #065f46; }
    .src-badge--user { background: #fef3c7; color: #92400e; }

    .actions { display: inline-flex; align-items: center; gap: 6px; flex-wrap: nowrap; }
    .action-btn { display: inline-flex; align-items: center; gap: 4px; padding: 5px 9px; border-radius: 6px; border: 1px solid transparent; font-size: 0.74rem; font-weight: 600; line-height: 1; cursor: pointer; transition: all .15s; white-space: nowrap; }
    .action-btn ion-icon { font-size: 0.95rem; }
    .action-btn--icon { padding: 6px 8px; }
    .action-btn--icon ion-icon { font-size: 1.05rem; }
    .action-btn--success { background: #12b76a; color: #fff; }
    .action-btn--success:hover:not(:disabled) { background: #039855; }
    .auto-note { display: inline-flex; align-items: center; gap: 4px; font-size: 11px; font-weight: 600; color: #0e7490; background: #ecfeff; padding: 4px 10px; border-radius: 999px; }
    .auto-note ion-icon { font-size: 13px; }
    .action-btn--nsano { background: #f79009; color: #fff; }
    .action-btn--nsano:hover:not(:disabled) { background: #dc6803; }
    .action-btn--zeepay { background: #06aed4; color: #fff; }
    .action-btn--zeepay:hover:not(:disabled) { background: #088ab2; }
    .action-btn--info { background: #fff; color: #667085; border-color: #d0d5dd; }
    .action-btn--info:hover { background: #f9fafb; color: #003377; border-color: #98a2b3; }
    .action-btn:disabled { opacity: 0.55; cursor: not-allowed; }
  `]
})
export class PartnerTransactionsPage implements OnInit {
  payoutTxns: any[] = [];
  payinTxns: any[] = [];
  loadingPayout = true;
  loadingPayin = true;
  activeTab: 'payout' | 'payin' = 'payout';

  constructor(
    private partnerService: PartnerService,
    private toastCtrl: ToastController,
    private alertCtrl: AlertController
  ) {}

  async viewBeneficiary(txn: any): Promise<void> {
    const dash = (v: any) => (v !== null && v !== undefined && String(v).trim() !== '' ? v : '—');
    const phone   = txn.beneficiaryPhone || txn.beneficiaryMobileNumber;
    const country = txn.beneficiaryCountry;
    const city    = txn.beneficiaryCity || txn.beneficiaryBranchCity;
    const bank    = txn.beneficiaryBankName || txn.beneficiaryBank;
    const account = txn.beneficiaryAccountNumber || txn.beneficiaryAccount;
    const branch  = txn.beneficiaryBranch || txn.beneficiarySortCode;
    const swift   = txn.beneficiarySwift || txn.beneficiarySwiftBic;
    const iban    = txn.beneficiaryIban;
    const provider = txn.beneficiaryProvider || txn.beneficiaryMobileProvider;
    const address = txn.beneficiaryAddress;
    const alert = await this.alertCtrl.create({
      header: 'Beneficiary Details',
      subHeader: txn.beneficiaryName || '',
      cssClass: 'fb-beneficiary-alert',
      message: `<div style="text-align:left;line-height:1.7">
        <b>Reference:</b> ${dash(txn.referenceNumber)}<br>
        <b>Telephone:</b> ${dash(phone)}<br>
        <b>Country:</b> ${dash(country)}<br>
        <b>City:</b> ${dash(city)}<br>
        <b>Address:</b> ${dash(address)}<br>
        <b>Bank Name:</b> ${dash(bank)}<br>
        <b>Account Number:</b> ${dash(account)}<br>
        <b>Branch:</b> ${dash(branch)}<br>
        <b>Swift Code:</b> ${dash(swift)}<br>
        <b>IBAN:</b> ${dash(iban)}<br>
        <b>Mobile Provider:</b> ${dash(provider)}
      </div>`,
      buttons: ['Close']
    });
    await alert.present();
  }

  ngOnInit(): void {
    this.loadAll();
  }

  loadAll(): void {
    this.loadPayoutTransactions();
    this.loadPayinTransactions();
  }

  loadPayoutTransactions(): void {
    this.loadingPayout = true;
    this.partnerService.getMyTransactions().subscribe({
      next: (res: any) => {
        const data = res?.data || res;
        this.payoutTxns = Array.isArray(data) ? data : (data?.content || []);
        this.loadingPayout = false;
      },
      error: () => {
        this.payoutTxns = [];
        this.loadingPayout = false;
      }
    });
  }

  loadPayinTransactions(): void {
    this.loadingPayin = true;
    this.partnerService.getPayinProcessingTransactions().subscribe({
      next: (res: any) => {
        this.payinTxns = Array.isArray(res) ? res : (res?.data || []);
        this.loadingPayin = false;
      },
      error: () => {
        this.payinTxns = [];
        this.loadingPayin = false;
      }
    });
  }

  markPayoutPaid(txn: any): void {
    txn._marking = true;
    this.partnerService.markPaid(txn.id).subscribe({
      next: () => {
        this.showToast('Transaction marked as paid', 'success');
        this.loadPayoutTransactions();
      },
      error: () => {
        txn._marking = false;
        this.showToast('Failed to update status', 'danger');
      }
    });
  }

  // Ghana payout via Nsano — builds the disbursement from the beneficiary (like the old
  // website) and, on Nsano success code "00", the backend marks the transaction PAID.
  payViaNsano(txn: any): void {
    txn._marking = true;
    this.partnerService.nsanoDisburse(txn.referenceNumber).subscribe({
      next: (res: any) => {
        const code = res?.code;
        if (code === '00') {
          this.showToast(`Nsano payout successful — ${txn.referenceNumber} marked Paid`, 'success');
          this.loadPayoutTransactions();
        } else {
          txn._marking = false;
          this.showToast(`Nsano: ${res?.msg || 'payout failed'} (code ${code || '—'})`, 'danger');
        }
      },
      error: (err: any) => {
        txn._marking = false;
        this.showToast(err?.error?.message || 'Nsano request failed', 'danger');
      }
    });
  }

  // Zeepay supports Ghana, Zimbabwe, Zambia and Nigeria payouts.
  isZeepayCurrency(cur: string): boolean {
    return ['GHS', 'ZWL', 'ZWG', 'ZMW', 'NGN'].includes(cur);
  }

  // Pay via Zeepay — builds the disbursement from the beneficiary (like the old website).
  // On Zeepay code "411" the transaction moves to SENT_TO_PAYOUT; final PAID arrives via polling.
  payViaZeepay(txn: any): void {
    txn._marking = true;
    this.partnerService.zeepayDisburse(txn.referenceNumber).subscribe({
      next: (res: any) => {
        txn._marking = false;
        if (res?.code === '411') {
          this.showToast(`Zeepay accepted ${txn.referenceNumber} — sent to payout`, 'success');
          this.loadPayoutTransactions();
        } else {
          this.showToast(`Zeepay: ${res?.message || res?.msg || 'payout failed'} (code ${res?.code || '—'})`, 'danger');
        }
      },
      error: (err: any) => {
        txn._marking = false;
        this.showToast(err?.error?.message || 'Zeepay request failed', 'danger');
      }
    });
  }

  markPayinPaid(txn: any): void {
    txn._marking = true;
    this.partnerService.markPayinTransactionPaid(txn.transactionId).subscribe({
      next: () => {
        this.showToast(`Pay-In transaction marked as paid`, 'success');
        this.payinTxns = this.payinTxns.filter(t => t.transactionId !== txn.transactionId);
      },
      error: (err: any) => {
        txn._marking = false;
        const msg = err?.error?.message || 'Failed to update status';
        this.showToast(msg, 'danger');
      }
    });
  }

  private async showToast(message: string, color: string): Promise<void> {
    const toast = await this.toastCtrl.create({
      message, duration: 4000, position: 'top', color,
      cssClass: `fb-toast fb-toast-${color}`,
      buttons: [{ icon: 'close-outline', role: 'cancel' }]
    });
    await toast.present();
  }
}
