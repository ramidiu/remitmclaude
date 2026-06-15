import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { PartnerService } from '../../core/services/partner.service';

@Component({
  selector: 'app-payin-partner-layout',
  template: `
    <div class="payin-layout" [class.collapsed]="sidenavCollapsed" [class.mobile-open]="mobileMenuOpen">
      <div class="payin-backdrop" *ngIf="mobileMenuOpen" (click)="closeMobileMenu()"></div>
      <!-- Sidenav -->
      <aside class="payin-sidenav">
        <div class="payin-sidenav__header">
          <div class="payin-sidenav__logo">
            <img src="assets/images/remitm-logo-white.svg" alt="Remitm Money Transfer" [style.width]="sidenavCollapsed ? '40px' : '140px'" style="height:auto;" />
          </div>
          <span class="payin-sidenav__role" *ngIf="!sidenavCollapsed">Pay-In Partner</span>
          <span class="payin-sidenav__admin-badge" *ngIf="!sidenavCollapsed && isAdminViewing">Viewing: {{ adminPartnerName }}</span>
          <button class="payin-sidenav__toggle" (click)="toggleSidenav()">
            <ion-icon [name]="sidenavCollapsed ? 'chevron-forward' : 'chevron-back'"></ion-icon>
          </button>
        </div>

        <nav class="payin-sidenav__nav">
          <a
            *ngFor="let item of visibleMenuItems"
            [routerLink]="item.route"
            class="payin-sidenav__item"
            [class.active]="isActive(item.route)"
            (click)="onNavClick()"
          >
            <ion-icon [name]="item.icon"></ion-icon>
            <span *ngIf="!sidenavCollapsed">{{ item.label }}</span>
          </a>
        </nav>

        <div class="payin-sidenav__footer">
          <a class="payin-sidenav__item payin-sidenav__item--switch" *ngIf="isAdminViewing" (click)="switchToAdmin()">
            <ion-icon name="arrow-back-outline"></ion-icon>
            <span *ngIf="!sidenavCollapsed">Switch to Admin</span>
          </a>
          <div *ngIf="isAdminViewing" style="border-top:1px solid rgba(255,255,255,0.1);margin:4px 12px;"></div>
          <a class="payin-sidenav__item" (click)="logout()">
            <ion-icon name="log-out-outline"></ion-icon>
            <span *ngIf="!sidenavCollapsed">Sign Out</span>
          </a>
        </div>
      </aside>

      <!-- Main Content -->
      <div class="payin-main">
        <!-- Top Toolbar -->
        <header class="payin-toolbar">
          <div class="payin-toolbar__left">
            <button class="payin-toolbar__menu-btn" (click)="toggleMobileMenu()">
              <ion-icon name="menu-outline"></ion-icon>
            </button>
          </div>
          <div class="payin-toolbar__right">
            <button class="payin-toolbar__notification-btn">
              <ion-icon name="notifications-outline"></ion-icon>
            </button>
            <div class="payin-toolbar__user">
              <div class="payin-toolbar__avatar">{{ initials }}</div>
              <span class="payin-toolbar__username">{{ userName }}</span>
            </div>
          </div>
        </header>

        <!-- Router Outlet -->
        <main class="payin-content">
          <router-outlet></router-outlet>
        </main>
      </div>
    </div>
  `,
  styleUrls: ['./payin-partner-layout.component.scss']
})
export class PayinPartnerLayoutComponent implements OnInit {
  sidenavCollapsed = false;
  mobileMenuOpen = false;

  // Super-admin toggles (default true until the flags load).
  customerCreationEnabled = true;
  transactionCreationEnabled = true;

  menuItems = [
    { label: 'Dashboard', icon: 'grid-outline', route: '/payin-partner/dashboard' },
    { label: 'Customers', icon: 'people-outline', route: '/payin-partner/customers', requires: 'customer' },
    { label: 'Create Customer', icon: 'person-add-outline', route: '/payin-partner/create-customer', requires: 'customer' },
    { label: 'Transactions', icon: 'swap-horizontal-outline', route: '/payin-partner/transactions', requires: 'transaction' },
    { label: 'Create Transaction', icon: 'add-circle-outline', route: '/payin-partner/create-transaction', requires: 'transaction' },
    { label: 'Ledger', icon: 'book-outline', route: '/payin-partner/ledger' },
    { label: 'Settlements', icon: 'wallet-outline', route: '/payin-partner/settlements' }
  ];

  constructor(
    public authService: AuthService,
    private router: Router,
    private partnerService: PartnerService
  ) {}

  ngOnInit(): void {
    this.partnerService.getPayinCreationFlags().subscribe({
      next: (f: any) => {
        this.customerCreationEnabled = f?.customerCreation !== false;
        this.transactionCreationEnabled = f?.transactionCreation !== false;
      },
      error: () => { /* default to enabled on error — backend still enforces */ }
    });
  }

  /** Menu with the Create options hidden when the super-admin has disabled them. */
  get visibleMenuItems(): any[] {
    return this.menuItems.filter(i =>
      !(i.requires === 'customer' && !this.customerCreationEnabled) &&
      !(i.requires === 'transaction' && !this.transactionCreationEnabled));
  }

  get isAdminViewing(): boolean {
    return !!sessionStorage.getItem('fb_admin_return');
  }

  get adminReturnRoute(): string {
    return sessionStorage.getItem('fb_admin_return') || '/admin';
  }

  get adminPartnerName(): string {
    return sessionStorage.getItem('fb_admin_partner_name') || 'Partner';
  }

  switchToAdmin(): void {
    const route = this.adminReturnRoute;
    sessionStorage.removeItem('fb_admin_return');
    sessionStorage.removeItem('fb_admin_partner_id');
    sessionStorage.removeItem('fb_admin_partner_name');
    window.location.href = route;
  }

  get initials(): string {
    const user = this.authService.getCurrentUser();
    if (!user) return 'PI';
    return (user?.email || '').substring(0, 2).toUpperCase();
  }

  get userName(): string {
    const user = this.authService.getCurrentUser();
    return user ? user?.email?.split('@')[0] || 'Partner' : 'Partner';
  }

  toggleSidenav(): void {
    this.sidenavCollapsed = !this.sidenavCollapsed;
  }

  toggleMobileMenu(): void { this.mobileMenuOpen = !this.mobileMenuOpen; }
  closeMobileMenu(): void { this.mobileMenuOpen = false; }
  onNavClick(): void { if (window.innerWidth < 992) this.mobileMenuOpen = false; }

  isActive(route: string): boolean {
    return this.router.url.startsWith(route);
  }

  logout(): void {
    this.authService.logout();
  }
}
