import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';

export interface AddressSuggestion {
  addressId: string;
  addressText: string;
  entries: number;
}

export interface AddressDetail {
  street: string;
  city: string;
  postcode: string;
  state: string;
  fullAddress: string;
  address2: string;
}

/**
 * Address lookup service.
 *
 * The previous third-party address-autocomplete provider has been removed.
 * These methods are intentionally stubbed to return no suggestions so
 * every consumer continues to compile and the UI falls back to MANUAL address
 * entry. To re-enable autocomplete later, wire these methods to a new
 * address-lookup backend endpoint.
 */
@Injectable({ providedIn: 'root' })
export class AddressService {
  lookup(_query: string, _country = 'GB', _selected = ''): Observable<AddressSuggestion[]> {
    return of([]);
  }

  retrieve(_addressId: string, _country = 'GB'): Observable<AddressDetail> {
    return of({ street: '', city: '', postcode: '', state: '', fullAddress: '', address2: '' });
  }
}
