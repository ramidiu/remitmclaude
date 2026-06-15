import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { IonicModule } from '@ionic/angular';
import { RouterModule } from '@angular/router';
import { UserAgreementPage } from './user-agreement.page';

@NgModule({
  imports: [CommonModule, IonicModule, RouterModule.forChild([{ path: '', component: UserAgreementPage }])],
  declarations: [UserAgreementPage]
})
export class UserAgreementPageModule {}
