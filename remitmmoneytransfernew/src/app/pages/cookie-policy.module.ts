import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { IonicModule } from '@ionic/angular';
import { RouterModule } from '@angular/router';
import { CookiePolicyPage } from './cookie-policy.page';

@NgModule({
  imports: [CommonModule, IonicModule, RouterModule.forChild([{ path: '', component: CookiePolicyPage }])],
  declarations: [CookiePolicyPage]
})
export class CookiePolicyPageModule {}
