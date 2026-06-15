import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { IonicModule } from '@ionic/angular';
import { RouterModule } from '@angular/router';
import { DisclaimerPage } from './disclaimer.page';

@NgModule({
  imports: [CommonModule, IonicModule, RouterModule.forChild([{ path: '', component: DisclaimerPage }])],
  declarations: [DisclaimerPage]
})
export class DisclaimerPageModule {}
