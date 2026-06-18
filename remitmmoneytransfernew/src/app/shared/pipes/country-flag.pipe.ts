import { Pipe, PipeTransform } from '@angular/core';
import { toAlpha2 } from '../utils/country-codes';

@Pipe({ name: 'countryFlagUrl' })
export class CountryFlagUrlPipe implements PipeTransform {
  transform(alpha3: string, _size: number = 24): string {
    return `assets/flags/${toAlpha2(alpha3).toLowerCase()}.svg`;
  }
}

@Pipe({ name: 'countryFlagSvg' })
export class CountryFlagSvgPipe implements PipeTransform {
  transform(alpha3: string): string {
    return `assets/flags/${toAlpha2(alpha3).toLowerCase()}.svg`;
  }
}
