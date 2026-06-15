import { Component, OnInit } from '@angular/core';
import { ToastController } from '@ionic/angular';
import { PartnerService } from '../../core/services/partner.service';

/**
 * Payout Routing — the admin toggle for the gateway flow.
 * Each row is a corridor + delivery method (corridor_delivery_methods, the single source of truth
 * the PayoutRoutingService reads). Picking a payout partner sets which gateway runs for that route:
 * the recipient form, validation and disbursement all follow it, with no code change.
 */
@Component({
  selector: 'app-payout-routing',
  templateUrl: './payout-routing.page.html',
  styleUrls: ['./payout-routing.page.scss']
})
export class PayoutRoutingPage implements OnInit {
  rows: any[] = [];
  partners: any[] = [];
  loading = true;
  savingId: number | null = null;

  constructor(
    private partnerService: PartnerService,
    private toastCtrl: ToastController
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    // Load partners FIRST so the dropdown options exist before the rows bind their selected value
    // (otherwise the select can't match payoutPartnerId and falsely shows "Unassigned").
    this.partnerService.getRoutingPartners().subscribe({
      next: (res) => { this.partners = Array.isArray(res) ? res : res?.data || []; this.loadRows(); },
      error: () => { this.partners = []; this.loadRows(); }
    });
  }

  private loadRows(): void {
    this.partnerService.getPayoutRouting().subscribe({
      next: (res) => {
        this.rows = Array.isArray(res) ? res : res?.data || [];
        this.loading = false;
      },
      error: () => { this.rows = []; this.loading = false; }
    });
  }

  /** Resolve the gateway label for the partner currently selected on a row (for the live pill). */
  gatewayFor(partnerId: number | null): string {
    if (!partnerId) return 'MANUAL';
    const p = this.partners.find(x => x.id === partnerId);
    return p?.gateway || 'MANUAL';
  }

  onAssign(row: any, partnerId: number | null): void {
    // ngModel emits on initial bind too — ignore no-op changes so we don't PUT on page load.
    if ((partnerId ?? null) === (row.payoutPartnerId ?? null)) return;
    this.savingId = row.id;
    this.partnerService.setPayoutRouting(row.id, partnerId).subscribe({
      next: () => {
        row.payoutPartnerId = partnerId;
        const p = this.partners.find(x => x.id === partnerId);
        row.partnerName = p?.partnerName || null;
        row.gateway = p?.gateway || 'MANUAL';
        this.savingId = null;
        this.showToast('Routing updated', 'success');
      },
      error: () => { this.savingId = null; this.showToast('Failed to update routing', 'danger'); }
    });
  }

  prettyMethod(m: string): string {
    return (m || '').replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, c => c.toUpperCase());
  }

  private async showToast(message: string, color: string): Promise<void> {
    const toast = await this.toastCtrl.create({ message, duration: 3000, position: 'top', color, cssClass: `fb-toast fb-toast-${color}`, buttons: [{ icon: 'close-outline', role: 'cancel' }] });
    await toast.present();
  }
}
