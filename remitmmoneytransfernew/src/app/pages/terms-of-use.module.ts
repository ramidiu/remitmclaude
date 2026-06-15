import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { IonicModule } from '@ionic/angular';
import { RouterModule } from '@angular/router';
import { TermsOfUsePage } from './terms-of-use.page';

@NgModule({
  imports: [CommonModule, IonicModule, RouterModule.forChild([{ path: '', component: TermsOfUsePage }])],
  declarations: [TermsOfUsePage]
})
export class TermsOfUsePageModule {}
