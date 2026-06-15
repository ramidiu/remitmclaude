import { Component } from '@angular/core';

@Component({
  selector: 'app-product-information',
  template: `
  <ion-content><div class="info-page">
    <div class="info-hero">
      <a class="info-back" href="/">&larr; Back to Home</a>
      <h1>Product Information</h1>
      <p>Important information about our money remittance service</p>
    </div>
    <div class="info-body">
      <p class="info-muted">If any words are capitalised, please refer to the definitions in our Client Agreement &amp; Terms and Conditions.</p>

      <h2>1. General Information</h2>
      <h3>1.1 Service Provider</h3>
      <p>The money remittance service is provided by RemitM Ltd (RemitM) (CRN: 07956213), a privately owned limited company. RemitM is regulated by the Financial Conduct Authority in the UK as a Small Payment Institution (FRN: 584554).</p>

      <h3>1.2 No Financial Advice</h3>
      <p>The information contained in this document is general in nature and is provided purely for information purposes. Our website also contains useful historical data and some charting and research tools. We may also provide you with general oral advice about how foreign exchange transactions work during the course of our dealings with you.</p>
      <p>Please note however that none of the information we provide to you, either on our website or over the phone, will take into account your personal financial circumstances and needs. You will always need to exercise your own judgment and should obtain independent financial advice as to the amount, type and timing of any particular transaction you enter into with us.</p>

      <h3>1.3 Contact Us</h3>
      <p>If you require further information, or do not understand any part of this document or anything on our website, please contact us by telephone on 020 8556 0888 or by email at admin@remitm.com.</p>

      <h3>1.4 Client Agreement</h3>
      <p>You will need to enter into a Client Agreement with us before we provide you with our service. We have two client agreements: one for companies and one for individuals. You must ensure that you fully understand the terms set out in the relevant Client Agreement before you transact with us.</p>

      <h3>1.5 Money Laundering</h3>
      <p>RemitM is registered with HMRC as a Money Services Business (Registration No. XHML00000105906). Please see our Money Laundering Policy and Statement on our website at www.remitm.com.</p>

      <h2>2. The Service</h2>
      <p>RemitM provides a money transfer service that allows you to send funds to family, friends and associates abroad quickly, reliably and securely. We will tell you the applicable exchange rate and any fees before you confirm each transaction.</p>

      <h2>3. Margin, Fees &amp; Benefits</h2>
      <ul>
        <li>A transfer fee is charged based on the value of your transaction and is shown to you before you confirm.</li>
        <li>Our pricing is competitive and transparent, with no hidden charges.</li>
        <li>You benefit from a fast, secure and convenient way to send money overseas.</li>
      </ul>

      <h2>4. Risks</h2>
      <p>Foreign exchange rates can move quickly. You should consider the amount, type and timing of any transaction carefully and obtain independent financial advice where appropriate.</p>

      <h2>5. Applicable Laws &amp; Dispute Resolution</h2>
      <p>Our service is governed by the laws of England &amp; Wales. If you have a complaint, please refer to our Complaint Policy. We are committed to resolving any dispute fairly and promptly.</p>

      <p class="info-muted">Remitm Limited is a company registered in England &amp; Wales. Company Registration No: 07956213. Registered office: 20 Kirkdale Road, London, E11 1HP, United Kingdom. Tel: 020 8556 0888. E-mail: info@remitm.com. Website: www.remitm.com.</p>
      <p><a href="/contact-us">Contact us &rarr;</a></p>
    </div>
  </div></ion-content>`
})
export class ProductInformationPage {}
