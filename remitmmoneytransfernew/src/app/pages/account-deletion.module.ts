import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClientModule } from '@angular/common/http';
import { IonicModule } from '@ionic/angular';
import { RouterModule } from '@angular/router';
import { AccountDeletionPage } from './account-deletion.page';

@NgModule({
  imports: [CommonModule, FormsModule, HttpClientModule, IonicModule,
    RouterModule.forChild([{ path: '', component: AccountDeletionPage }])],
  declarations: [AccountDeletionPage]
})
export class AccountDeletionPageModule {}
