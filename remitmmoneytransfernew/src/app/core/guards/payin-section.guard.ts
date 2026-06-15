import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivate, Router, RouterStateSnapshot } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { PartnerService } from '../services/partner.service';

/**
 * Blocks direct-URL access to a pay-in customer/transaction page when the super-admin has turned
 * the matching toggle OFF (System Controls). Attach via route data:
 *   { canActivate: [PayinSectionGuard], data: { payinRequires: 'customer' | 'transaction' } }
 * The menu items are already hidden; this stops someone typing the URL. Backend also enforces creates.
 */
@Injectable({ providedIn: 'root' })
export class PayinSectionGuard implements CanActivate {
  constructor(private partnerService: PartnerService, private router: Router) {}

  async canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Promise<boolean> {
    const requires = route.data?.['payinRequires'];
    if (!requires) return true;
    try {
      const f: any = await firstValueFrom(this.partnerService.getPayinCreationFlags());
      const enabled = requires === 'customer'
        ? f?.customerCreation !== false
        : f?.transactionCreation !== false;
      if (!enabled) {
        const home = state.url.startsWith('/superadmin') ? '/superadmin/dashboard' : '/payin-partner/dashboard';
        this.router.navigate([home]);
        return false;
      }
      return true;
    } catch {
      return true; // allow on transient error — backend remains the source of truth
    }
  }
}
