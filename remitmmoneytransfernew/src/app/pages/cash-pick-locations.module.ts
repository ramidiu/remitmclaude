import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { IonicModule } from '@ionic/angular';
import { RouterModule } from '@angular/router';
import { CashPickLocationsPage } from './cash-pick-locations.page';

@NgModule({
  imports: [CommonModule, IonicModule, RouterModule.forChild([{ path: '', component: CashPickLocationsPage }])],
  declarations: [CashPickLocationsPage]
})
export class CashPickLocationsPageModule {}
