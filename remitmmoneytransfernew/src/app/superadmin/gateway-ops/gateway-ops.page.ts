import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ToastController } from '@ionic/angular';
import { Subject } from 'rxjs';
import { takeUntil, debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { PartnerService } from '../../core/services/partner.service';

/**
 * One reusable page for per-gateway operations. The route supplies `gateway` (NSANO / ZEEPAY),
 * so the same component powers the Nsano page and the Zeepay page — the page IS the rail, so the
 * operator can only act on this gateway's transactions (no wrong-rail risk). MANUAL has no page.
 */
@Component({
  selector: 'app-gateway-ops',
  templateUrl: './gateway-ops.page.html',
  styleUrls: ['./gateway-ops.page.scss']
})
export class GatewayOpsPage implements OnInit, OnDestroy {
  gateway = '';                 // NSANO | ZEEPAY
  scope: 'all' | 'pending' | 'done' | 'cancelled' = 'all';
  rows: any[] = [];
  summary: any = { pending: 0, paid: 0, paidTotalAmount: 0, cancelled: 0 };
  loading = true;
  search = '';                  // server-side search (reference / sender)

  // Server-side pagination
  page = 0;
  size = 25;
  totalPages = 0;
  totalElements = 0;

  // Pending/actionable statuses — drives whether the row shows a "Check Status" action,
  // independent of the active tab (so it works in the All view too).
  private readonly ACTIONABLE = ['PROCESSING', 'FUNDS_RECEIVED', 'SENT_TO_PAYOUT'];

  private destroy$ = new Subject<void>();
  private search$ = new Subject<string>();

  constructor(
    private route: ActivatedRoute,
    private partnerService: PartnerService,
    private toastCtrl: ToastController
  ) {}

  ngOnInit(): void {
    // Debounced server-side search — resets to page 0 and reloads.
    this.search$.pipe(debounceTime(350), distinctUntilChanged(), takeUntil(this.destroy$))
      .subscribe(() => { this.page = 0; this.load(); });

    // React to route param so navigating Nsano → Zeepay reuses the component cleanly.
    this.route.paramMap.pipe(takeUntil(this.destroy$)).subscribe(pm => {
      this.gateway = (pm.get('gateway') || '').toUpperCase();
      this.scope = 'all';
      this.search = '';
      this.page = 0;
      this.load();
    });
  }

  onSearchChange(): void { this.search$.next(this.search); }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get title(): string {
    return this.gateway === 'NSANO' ? 'Nsano' : this.gateway === 'ZEEPAY' ? 'Zeepay' : this.gateway;
  }

  setScope(scope: 'all' | 'pending' | 'done' | 'cancelled'): void {
    if (this.scope === scope) return;
    this.scope = scope;
    this.page = 0;
    this.load();
  }

  load(): void {
    if (!this.gateway) return;
    this.loading = true;
    this.partnerService.getGatewayTransactions(this.gateway, this.scope, this.page, this.size, this.search)
      .pipe(takeUntil(this.destroy$)).subscribe({
        next: (res) => {
          this.rows = res?.transactions || [];
          this.summary = res?.summary || { pending: 0, paid: 0, paidTotalAmount: 0, cancelled: 0 };
          this.totalPages = res?.totalPages || 0;
          this.totalElements = res?.totalElements || 0;
          this.loading = false;
        },
        error: () => { this.rows = []; this.loading = false; }
      });
  }

  goPage(delta: number): void {
    const next = this.page + delta;
    if (next < 0 || next >= this.totalPages) return;
    this.page = next;
    this.load();
  }

  /** A row is actionable (Check Status) when its CURRENT status is still in flight — any tab. */
  isActionable(status: string): boolean {
    return this.ACTIONABLE.includes((status || '').toUpperCase());
  }

  checkStatus(row: any): void {
    row._busy = true;
    this.partnerService.gatewayCheckStatus(row.referenceNumber).pipe(takeUntil(this.destroy$)).subscribe({
      next: (res: any) => {
        row._busy = false;
        const st = res?.status;
        this.showToast(res?.success ? `${this.title} status: ${st}` : (res?.message || 'Status check unavailable'),
          res?.success ? (st === 'PAID' ? 'success' : 'primary') : 'warning');
        this.load();
      },
      error: () => { row._busy = false; this.showToast('Status check failed', 'danger'); }
    });
  }

  private async showToast(message: string, color: string): Promise<void> {
    const toast = await this.toastCtrl.create({ message, duration: 3500, position: 'top', color, cssClass: `fb-toast fb-toast-${color}`, buttons: [{ icon: 'close-outline', role: 'cancel' }] });
    await toast.present();
  }
}
