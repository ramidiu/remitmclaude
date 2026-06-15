import { NgModule } from '@angular/core';
import { PreloadAllModules, RouterModule, Routes } from '@angular/router';
import { AuthGuard } from './core/guards/auth.guard';
import { RoleGuard } from './core/guards/role.guard';

const routes: Routes = [
  {
    path: '',
    loadChildren: () => import('./landing/landing.module').then(m => m.LandingPageModule)
  },
  // Public info / legal pages — each a lazy NgModule (loadChildren) so it renders under
  // <ion-router-outlet> on client-side (routerLink) navigation. (Ionic 6's ion-router-outlet
  // does not support standalone loadComponent, and direct component routes only render on a
  // full reload — so a lazy module per page is the pattern that works, like login/receipt.)
  { path: 'privacy-policy', loadChildren: () => import('./pages/privacy-policy.module').then(m => m.PrivacyPolicyPageModule) },
  { path: 'privacypolicy', loadChildren: () => import('./pages/privacy-policy.module').then(m => m.PrivacyPolicyPageModule) },
  { path: 'about-us', loadChildren: () => import('./pages/about.module').then(m => m.AboutPageModule) },
  { path: 'contact-us', loadChildren: () => import('./pages/contact-us.module').then(m => m.ContactUsPageModule) },
  { path: 'terms', loadChildren: () => import('./pages/terms.module').then(m => m.TermsPageModule) },
  { path: 'faq', loadChildren: () => import('./pages/faq.module').then(m => m.FaqPageModule) },
  { path: 'complaints', loadChildren: () => import('./pages/complaints.module').then(m => m.ComplaintsPageModule) },
  { path: 'cookie-policy', loadChildren: () => import('./pages/cookie-policy.module').then(m => m.CookiePolicyPageModule) },
  { path: 'user-agreement', loadChildren: () => import('./pages/user-agreement.module').then(m => m.UserAgreementPageModule) },
  { path: 'mobile-terms', loadChildren: () => import('./pages/mobile-terms.module').then(m => m.MobileTermsPageModule) },
  { path: 'mobile-privacy', loadChildren: () => import('./pages/mobile-privacy.module').then(m => m.MobilePrivacyPageModule) },
  { path: 'account-deletion', loadChildren: () => import('./pages/account-deletion.module').then(m => m.AccountDeletionPageModule) },
  { path: 'product-information', loadChildren: () => import('./pages/product-information.module').then(m => m.ProductInformationPageModule) },
  { path: 'disclaimer', loadChildren: () => import('./pages/disclaimer.module').then(m => m.DisclaimerPageModule) },
  { path: 'money-transfer-policy', loadChildren: () => import('./pages/money-transfer-policy.module').then(m => m.MoneyTransferPolicyPageModule) },
  { path: 'terms-of-use', loadChildren: () => import('./pages/terms-of-use.module').then(m => m.TermsOfUsePageModule) },
  { path: 'gdpr-privacy-policy', loadChildren: () => import('./pages/gdpr-privacy-policy.module').then(m => m.GdprPrivacyPolicyPageModule) },
  { path: 'cash-pick-locations', loadChildren: () => import('./pages/cash-pick-locations.module').then(m => m.CashPickLocationsPageModule) },
  // Staff receipt viewer (print + download PDF) — lazy module so it renders under ion-router-outlet on SPA nav.
  {
    path: 'receipt/:id',
    loadChildren: () => import('./pages/receipt-view.module').then(m => m.ReceiptViewModule),
    canActivate: [AuthGuard]
  },
  {
    path: 'login',
    loadChildren: () => import('./customer/login/login.module').then(m => m.LoginPageModule)
  },
  {
    path: 'admin-login',
    loadChildren: () => import('./customer/admin-login/admin-login.module').then(m => m.AdminLoginPageModule)
  },
  {
    path: 'register',
    loadChildren: () => import('./customer/register/register.module').then(m => m.RegisterPageModule)
  },
  {
    path: 'otp-verify',
    loadChildren: () => import('./customer/otp-verify/otp-verify.module').then(m => m.OtpVerifyPageModule)
  },
  {
    path: 'forgot-password',
    loadChildren: () => import('./customer/forgot-password/forgot-password.module').then(m => m.ForgotPasswordPageModule)
  },
  {
    path: 'reset-password',
    loadChildren: () => import('./customer/reset-password/reset-password.module').then(m => m.ResetPasswordPageModule)
  },
  {
    path: 'admin-mfa-setup',
    loadChildren: () => import('./customer/admin-mfa-setup/admin-mfa-setup.module').then(m => m.AdminMfaSetupPageModule)
  },
  {
    path: 'demo-access',
    loadChildren: () => import('./customer/demo-access/demo-access.module').then(m => m.DemoAccessPageModule)
  },
  {
    path: 'home',
    loadChildren: () => import('./customer/tabs/tabs.module').then(m => m.TabsPageModule),
    canActivate: [AuthGuard]
  },
  {
    path: 'admin',
    loadChildren: () => import('./admin/admin.module').then(m => m.AdminModule),
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['ADMIN', 'SUPER_ADMIN'] }
  },
  {
    path: 'partner',
    loadChildren: () => import('./partner/partner.module').then(m => m.PartnerModule),
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['PAYOUT_PARTNER'] }
  },
  {
    path: 'agent',
    loadChildren: () => import('./agent/agent.module').then(m => m.AgentModule),
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['AGENT'] }
  },
  {
    path: 'superadmin',
    loadChildren: () => import('./superadmin/superadmin.module').then(m => m.SuperAdminModule),
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['SUPER_ADMIN', 'ADMIN'] }
  },
  {
    path: 'payin-partner',
    loadChildren: () => import('./payin-partner/payin-partner.module').then(m => m.PayinPartnerModule),
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['PAYIN_PARTNER'] }
  },
  { path: '**', redirectTo: '' }
];

@NgModule({
  imports: [
    RouterModule.forRoot(routes, { preloadingStrategy: PreloadAllModules })
  ],
  exports: [RouterModule]
})
export class AppRoutingModule {}
