import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';

@Injectable({ providedIn: 'root' })
export class PartnerService {
  private baseUrl = `${environment.apiUrl}/transactions`;

  constructor(private http: HttpClient) {}

  // Pay-in creation feature flags (super-admin toggles in System Controls). Drives whether the
  // pay-in partner portal shows "Create Customer" / "Create Transaction".
  getPayinCreationFlags(): Observable<any> { return this.http.get(`${environment.apiUrl}/payin/creation-flags`); }

  // Gateway operations (admin per-rail pages: Nsano / Zeepay). Each page = one gateway only.
  getGatewayTransactions(gateway: string, scope: 'all' | 'pending' | 'done' | 'cancelled' = 'all',
                         page = 0, size = 25, search = ''): Observable<any> {
    let params = new HttpParams().set('scope', scope).set('page', page).set('size', size);
    if (search && search.trim()) params = params.set('search', search.trim());
    return this.http.get(`${environment.apiUrl}/payout/gateway/${gateway}/transactions`, { params });
  }
  gatewayRetry(referenceNumber: string): Observable<any> {
    return this.http.post(`${environment.apiUrl}/payout/disburse/${referenceNumber}`, {});
  }
  gatewayCheckStatus(referenceNumber: string): Observable<any> {
    return this.http.post(`${environment.apiUrl}/payout/gateway/check-status/${referenceNumber}`, {});
  }

  // Payout Routing — assign corridor_delivery_methods → payout partner (drives the gateway).
  getPayoutRouting(): Observable<any> { return this.http.get(`${this.baseUrl}/payout-routing`); }
  getRoutingPartners(): Observable<any> { return this.http.get(`${this.baseUrl}/payout-routing/partners`); }
  setPayoutRouting(id: number, payoutPartnerId: number | null): Observable<any> {
    return this.http.put(`${this.baseUrl}/payout-routing/${id}`, { payoutPartnerId });
  }

  // Payout Partners
  getPayoutPartners(): Observable<any> { return this.http.get(`${this.baseUrl}/partners`); }
  createPayoutPartner(data: any): Observable<any> { return this.http.post(`${this.baseUrl}/partners`, data); }
  updatePayoutPartner(id: number, data: any): Observable<any> { return this.http.put(`${this.baseUrl}/partners/${id}`, data); }
  togglePayoutPartner(id: number): Observable<any> { return this.http.put(`${this.baseUrl}/partners/${id}/toggle`, {}); }
  getPartnerCountries(id: number): Observable<any> { return this.http.get(`${this.baseUrl}/partners/${id}/countries`); }
  assignCountry(id: number, data: any): Observable<any> { return this.http.post(`${this.baseUrl}/partners/${id}/countries`, data); }
  removeCountry(partnerId: number, countryId: number): Observable<any> { return this.http.delete(`${this.baseUrl}/partners/${partnerId}/countries/${countryId}`); }

  // Payout partner portal
  getMyTransactions(): Observable<any> { return this.http.get(`${this.baseUrl}/partners/my-transactions`); }
  getMyCompleted(): Observable<any> { return this.http.get(`${this.baseUrl}/partners/my-completed`); }
  getMyLedger(): Observable<any> { return this.http.get(`${this.baseUrl}/partners/my-ledger`); }
  markPaid(txnId: number): Observable<any> { return this.http.put(`${this.baseUrl}/partners/payout/${txnId}`, {}); }
  // Auto-disburse a Ghana payout via Nsano (builds the request from the beneficiary, like the
  // old website). On Nsano code "00" the backend marks the transaction PAID.
  nsanoDisburse(referenceNumber: string): Observable<any> { return this.http.post(`${environment.apiUrl}/payout/nsano/disburse/${referenceNumber}`, {}); }
  // Auto-disburse via Zeepay (Ghana/Zimbabwe/Zambia/Nigeria), built from the beneficiary. On
  // Zeepay code "411" the backend moves the transaction to SENT_TO_PAYOUT (PAID via polling).
  zeepayDisburse(referenceNumber: string): Observable<any> { return this.http.post(`${environment.apiUrl}/payout/zeepay/disburse/${referenceNumber}`, {}); }

  // Pay-in Partners
  getPayinPartners(): Observable<any> { return this.http.get(`${this.baseUrl}/corridors/payin-partners`); }
  createPayinPartner(data: any): Observable<any> { return this.http.post(`${this.baseUrl}/corridors/payin-partners`, data); }
  updatePayinPartner(id: number, data: any): Observable<any> { return this.http.put(`${this.baseUrl}/corridors/payin-partners/${id}`, data); }
  getPayinTransactions(): Observable<any> { return this.http.get(`${this.baseUrl}/corridors/my-transactions`); }
  payinApproveTransaction(id: number, paymentReference?: string): Observable<any> {
    return this.http.put(`${this.baseUrl}/corridors/my-transactions/${id}/received`, { paymentReference });
  }
  payinRejectTransaction(id: number, reason?: string): Observable<any> {
    return this.http.put(`${this.baseUrl}/corridors/my-transactions/${id}/reject`, { reason });
  }
  payinReleaseCompliance(id: number, reason?: string): Observable<any> {
    return this.http.put(`${this.baseUrl}/corridors/my-transactions/${id}/release-compliance`, { reason });
  }
  getPayinLedger(): Observable<any> { return this.http.get(`${this.baseUrl}/corridors/my-ledger`); }
  getPayinBalances(): Observable<any> { return this.http.get(`${this.baseUrl}/corridors/payin-balances`); }
  getPayinCustomers(): Observable<any> { return this.http.get(`${environment.apiUrl}/payin/customer/list`); }
  toggleFrontendCustomerPayin(userId: number): Observable<any> { return this.http.put(`${environment.apiUrl}/payin/customer/user/${userId}/payin-toggle`, {}); }
  createPayinTransaction(data: any): Observable<any> { return this.http.post(`${environment.apiUrl}/payin/transaction/create`, data); }
  getPayinTransactionList(): Observable<any> { return this.http.get(`${environment.apiUrl}/payin/transaction/list`); }
  getPayinProcessingTransactions(): Observable<any> { return this.http.get(`${environment.apiUrl}/payin/transaction/processing`); }
  markPayinTransactionPaid(transactionId: string): Observable<any> { return this.http.put(`${environment.apiUrl}/payin/transaction/${transactionId}/mark-paid`, {}); }
  downloadPayinReceipt(transactionId: string): Observable<Blob> { return this.http.get(`${environment.apiUrl}/payin/transaction/${transactionId}/receipt.pdf`, { responseType: 'blob' }); }
  getPayinBeneficiariesForCustomer(customerId: string): Observable<any> { return this.http.get(`${environment.apiUrl}/payin/beneficiary/list/${customerId}`); }
  getPayinCustomerDocuments(customerId: string): Observable<any> { return this.http.get(`${environment.apiUrl}/payin/customer/${customerId}/documents`); }

  /** Upload a KYC document for a payin customer. docCategory: IDENTITY|ADDRESS, docSide: FRONT|BACK */
  uploadPayinCustomerDocument(customerId: string, file: File, docCategory: string, docSide: string,
                              documentNumber?: string, issueDate?: string, expiryDate?: string): Observable<any> {
    const fd = new FormData();
    fd.append('file', file);
    fd.append('docCategory', docCategory);
    fd.append('docSide', docSide);
    if (documentNumber) fd.append('documentNumber', documentNumber);
    if (issueDate)      fd.append('issueDate', issueDate);
    if (expiryDate)     fd.append('expiryDate', expiryDate);
    return this.http.post(`${environment.apiUrl}/payin/customer/${customerId}/document`, fd);
  }

  /** Update DOB / verified status on a payin customer. */
  updatePayinCustomerProfile(customerId: string, dob?: string, isVerified?: boolean): Observable<any> {
    const body: any = {};
    if (dob !== undefined)        body.dob = dob;
    if (isVerified !== undefined) body.isVerified = isVerified;
    return this.http.put(`${environment.apiUrl}/payin/customer/${customerId}/profile`, body);
  }

  /** Existing KYC data (DOB + ID + address proof) already on file for a customer. */
  getExistingKyc(customerId: string): Observable<any> {
    return this.http.get(`${environment.apiUrl}/payin/customer/${customerId}/kyc-existing`);
  }

  /** Approve a customer using documents already on file (no re-upload). */
  verifyExistingCustomer(customerId: string): Observable<any> {
    return this.http.post(`${environment.apiUrl}/payin/customer/${customerId}/verify`, {});
  }

  /** Generic authenticated blob fetch — used to preview KYC documents inline. */
  fetchBlob(url: string): Observable<Blob> {
    return this.http.get(url, { responseType: 'blob' });
  }

  // Corridor mappings
  getCorridorMappings(): Observable<any> { return this.http.get(`${this.baseUrl}/partners/corridor-mappings`); }
  createCorridorMapping(data: any): Observable<any> { return this.http.post(`${this.baseUrl}/partners/corridor-mappings`, data); }
  deleteCorridorMapping(id: number): Observable<any> { return this.http.delete(`${this.baseUrl}/partners/corridor-mappings/${id}`); }

  // Corridor fee configs
  getCorridorConfigs(): Observable<any> { return this.http.get(`${this.baseUrl}/partners/corridor-configs`); }
  updateCorridorConfig(from: string, to: string, data: any): Observable<any> { return this.http.put(`${this.baseUrl}/partners/corridor-configs/${from}/${to}`, data); }
}
