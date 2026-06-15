import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { IonicModule } from '@ionic/angular';
import { RouterModule } from '@angular/router';
import { PrivacyPolicyPage } from './privacy-policy.page';

@NgModule({
  imports: [CommonModule, IonicModule, RouterModule.forChild([{ path: '', component: PrivacyPolicyPage }])],
  declarations: [PrivacyPolicyPage]
})
export class PrivacyPolicyPageModule {}
