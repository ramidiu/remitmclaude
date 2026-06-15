import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { IonicModule } from '@ionic/angular';
import { RouterModule } from '@angular/router';
import { ProductInformationPage } from './product-information.page';

@NgModule({
  imports: [CommonModule, IonicModule, RouterModule.forChild([{ path: '', component: ProductInformationPage }])],
  declarations: [ProductInformationPage]
})
export class ProductInformationPageModule {}
