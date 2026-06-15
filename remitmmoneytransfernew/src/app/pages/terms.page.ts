import { Component } from '@angular/core';

@Component({
  selector: 'app-terms',
  template: `
  <ion-content><div class="info-page">
    <div class="info-hero">
      <a class="info-back" href="/">&larr; Back to Home</a>
      <h1>Terms of Use</h1>
      <p>Effective Date: 24 June 2025</p>
    </div>
    <div class="info-body">
      <p>Remitm Limited, a company incorporated in England with registration number 07956213, whose registered office is 20 Kirkdale Road, London, E11 1HP, is authorised and regulated by the Financial Conduct Authority (FCA) under the Payment Services Regulations as a Small Payment Institution, FCA registration number 584554, trading under the name "RemitM" (hereinafter "we", "our" or "us"). RemitM is also a registered Money Services Business with HM Revenue &amp; Customs (No. XHML00000105906).</p>
      <p>These Terms and Conditions are applicable to and available on our website www.remitm.com. They explain your responsibilities to us and our responsibilities to you, how and when our agreement with you can be terminated and the extent of our liability to you. When you register on our website and use any of our services described in these Terms and Conditions, you are agreeing to be bound by them. Please do not use any of our services if you do not accept these Terms and Conditions.</p>

      <h2>1. Overview &amp; Eligibility</h2>
      <p>These Terms and Conditions govern the provision by us of payment services consisting of the execution of payment transactions ordered by you through our website. Our obligations are conditional upon our acceptance of you as a user, which is at our sole discretion. You must be at least 18 years old and legally able to enter into a binding contract, and you must provide accurate, current and complete information when registering and using the service.</p>

      <h2>2. Identity Verification (KYC/AML)</h2>
      <p>As a regulated payment institution, we are required to verify your identity and may request documents and information before processing transactions. You must promptly supply us with all information and documentation we may ask for to enable us to comply with our legal requirements, including anti-money laundering and anti-terrorist financing regulation. We may decline, delay, or refund a transaction where verification cannot be completed or where we suspect fraud or unlawful activity.</p>

      <h2>3. Issue of Payment Orders &amp; Currency Exchange</h2>
      <p>Exchange rates and fees are displayed before you confirm a transfer and apply to that transaction. Once a transfer is confirmed and funds are received, it is processed to the beneficiary you specify. You are responsible for ensuring beneficiary details are correct; we are not liable for transfers sent to incorrect details you provide.</p>

      <h2>4. Fees</h2>
      <p>Applicable fees are shown at the time of the transaction. We reserve the right to change our fees and rates at any time, but changes will not affect transactions already confirmed.</p>

      <h2>5. Cancellations and Refunds</h2>
      <p>You may request cancellation of a transfer that has not yet been paid out to the beneficiary. Refunds are made to the original payment source and may be subject to deductions for fees or currency fluctuations. Completed transfers cannot be reversed.</p>

      <h2>6. Acceptable Use</h2>
      <p>You agree not to use the Service for any unlawful purpose, including money laundering, terrorist financing, fraud, or sanctions evasion. We may suspend or terminate accounts that breach these terms or applicable law.</p>

      <h2>7. Limitation of Liability</h2>
      <p>To the extent permitted by law, Remitm is not liable for indirect or consequential losses, or for delays caused by third parties, payment networks, or incorrect information you provide. Nothing in these terms excludes liability that cannot be excluded under law.</p>

      <h2>8. Privacy</h2>
      <p>Your use of the Service is also governed by our <a href="/privacy-policy">Privacy Policy</a>, which explains how we handle your personal data.</p>

      <h2>9. Changes to These Terms</h2>
      <p>We may update these Terms from time to time. Continued use of the Service after changes are posted constitutes acceptance of the updated Terms.</p>

      <h2>10. Contact</h2>
      <p>Questions about these Terms? Call us on <a href="tel:+442085560888">020 8556 0888</a>, email <a href="mailto:info@remitm.com">info@remitm.com</a>, or see our <a href="/contact-us">Contact page</a>.</p>
      <p class="info-muted">Remitm Limited is a company registered in England &amp; Wales. Company Registration No: 07956213. FCA registration number: 584554. HMRC MSB No: XHML00000105906. Registered office: 20 Kirkdale Road, London, E11 1HP, United Kingdom.</p>
    </div>
  </div></ion-content>`
})
export class TermsPage {}
