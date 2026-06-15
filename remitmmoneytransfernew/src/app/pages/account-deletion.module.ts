import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { IonicModule } from '@ionic/angular';
import { RouterModule } from '@angular/router';
import { AccountDeletionPage } from './account-deletion.page';

@NgModule({
  imports: [CommonModule, IonicModule, RouterModule.forChild([{ path: '', component: AccountDeletionPage }])],
  declarations: [AccountDeletionPage]
})
export class AccountDeletionPageModule {}
