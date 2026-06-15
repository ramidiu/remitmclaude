import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { IonicModule } from '@ionic/angular';
import { RouterModule } from '@angular/router';
import { GdprPrivacyPolicyPage } from './gdpr-privacy-policy.page';

@NgModule({
  imports: [CommonModule, IonicModule, RouterModule.forChild([{ path: '', component: GdprPrivacyPolicyPage }])],
  declarations: [GdprPrivacyPolicyPage]
})
export class GdprPrivacyPolicyPageModule {}
