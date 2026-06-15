import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { IonicModule } from '@ionic/angular';
import { RouterModule } from '@angular/router';
import { MoneyTransferPolicyPage } from './money-transfer-policy.page';

@NgModule({
  imports: [CommonModule, IonicModule, RouterModule.forChild([{ path: '', component: MoneyTransferPolicyPage }])],
  declarations: [MoneyTransferPolicyPage]
})
export class MoneyTransferPolicyPageModule {}
