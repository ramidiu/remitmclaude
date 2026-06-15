import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { IonicModule } from '@ionic/angular';
import { RouterModule } from '@angular/router';
import { ComplaintsPage } from './complaints.page';

@NgModule({
  imports: [CommonModule, IonicModule, RouterModule.forChild([{ path: '', component: ComplaintsPage }])],
  declarations: [ComplaintsPage]
})
export class ComplaintsPageModule {}
