import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { IonicModule } from '@ionic/angular';
import { RouterModule } from '@angular/router';
import { MobilePrivacyPage } from './mobile-privacy.page';

@NgModule({
  imports: [CommonModule, IonicModule, RouterModule.forChild([{ path: '', component: MobilePrivacyPage }])],
  declarations: [MobilePrivacyPage]
})
export class MobilePrivacyPageModule {}
