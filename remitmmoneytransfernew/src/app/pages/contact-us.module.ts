import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { IonicModule } from '@ionic/angular';
import { RouterModule } from '@angular/router';
import { ContactUsPage } from './contact-us.page';

@NgModule({
  imports: [CommonModule, IonicModule, RouterModule.forChild([{ path: '', component: ContactUsPage }])],
  declarations: [ContactUsPage]
})
export class ContactUsPageModule {}
