import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AddBeneficiaryRequest,
  UpdateBeneficiaryRequest,
  BeneficiaryResponse
} from '../models/beneficiary.model';

@Injectable({
  providedIn: 'root'
})
export class BeneficiaryService {
  private readonly baseUrl = `${environment.apiUrl}/beneficiaries`;

  constructor(private http: HttpClient) {}

  // ---- Gateway-agnostic recipient validation (backend resolves Nsano/Zeepay/Manual from the
  //      corridor → payout partner → gateway; the frontend never names a gateway) ----
  validateRecipientGeneric(receiveCurrency: string, deliveryMethod: string, accountNumber: string, bankOrProvider?: string, routingNumber?: string): Observable<any> {
    let params = new HttpParams()
      .set('receiveCurrency', receiveCurrency)
      .set('deliveryMethod', deliveryMethod)
      .set('accountNumber', accountNumber);
    if (bankOrProvider) params = params.set('bankOrProvider', bankOrProvider);
    if (routingNumber) params = params.set('routingNumber', routingNumber);
    return this.http.get(`${environment.apiUrl}/payout/validate`, { params });
  }
  /** Resolve which gateway/capabilities apply to a corridor + delivery method. */
  getPayoutRoute(receiveCurrency: string, deliveryMethod: string): Observable<any> {
    const params = new HttpParams().set('receiveCurrency', receiveCurrency).set('deliveryMethod', deliveryMethod);
    return this.http.get(`${environment.apiUrl}/payout/route`, { params });
  }

  // ---- Legacy gateway-specific validation (kept for reference; superseded by the facade) ----
  /** Nsano name lookup. kind = 'wallet' (provider+mobile) or 'account' (bank+account). */
  nsanoNameCheck(kind: 'wallet' | 'account', destinationHouse: string, accountNumber: string): Observable<any> {
    const params = new HttpParams().set('type', kind).set('destinationHouse', destinationHouse).set('accountNumber', accountNumber);
    return this.http.get(`${environment.apiUrl}/payout/nsano/name-check`, { params });
  }
  /** Zeepay mobile-wallet validation (mno + mobile number → name). */
  zeepayValidateWallet(mno: string, mobileNumber: string): Observable<any> {
    const params = new HttpParams().set('mno', mno).set('mobileNumber', mobileNumber);
    return this.http.get(`${environment.apiUrl}/payout/zeepay/validate/wallet`, { params });
  }
  /** Zeepay bank validation (routing + account + country → name). */
  zeepayValidateBank(routingNumber: string, accountNumber: string, receivingCountry: string): Observable<any> {
    const params = new HttpParams().set('routingNumber', routingNumber).set('accountNumber', accountNumber).set('receivingCountry', receivingCountry);
    return this.http.get(`${environment.apiUrl}/payout/zeepay/validate/bank`, { params });
  }

  add(request: AddBeneficiaryRequest): Observable<BeneficiaryResponse> {
    return this.http.post<any>(this.baseUrl, request).pipe(
      map(res => res.data || res)
    );
  }

  list(): Observable<BeneficiaryResponse[]> {
    return this.http.get<any>(this.baseUrl).pipe(
      map(res => res || [])
    );
  }

  getById(id: string): Observable<BeneficiaryResponse> {
    return this.http.get<any>(`${this.baseUrl}/${id}`).pipe(
      map(res => res.data || res)
    );
  }

  update(id: string, request: UpdateBeneficiaryRequest): Observable<BeneficiaryResponse> {
    return this.http.put<any>(`${this.baseUrl}/${id}`, request).pipe(
      map(res => res.data || res)
    );
  }

  delete(id: string): Observable<any> {
    return this.http.delete(`${this.baseUrl}/${id}`);
  }

  toggleFavourite(id: string): Observable<any> {
    return this.http.put(`${this.baseUrl}/${id}`, { isFavourite: true });
  }

  getFavourites(): Observable<BeneficiaryResponse[]> {
    return this.http.get<any>(this.baseUrl, {
      params: new HttpParams().set('favourite', 'true')
    }).pipe(
      map(res => res || [])
    );
  }
}
