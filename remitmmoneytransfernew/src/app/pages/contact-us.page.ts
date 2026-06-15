import { Component } from '@angular/core';

@Component({
  selector: 'app-contact-us',
  template: `
  <ion-content><div class="info-page">
    <div class="info-hero">
      <a class="info-back" href="/">&larr; Back to Home</a>
      <h1>Contact Us</h1>
      <p>We're here to help — get in touch</p>
    </div>
    <div class="info-body">
      <div class="info-contact-row">
        <ion-icon name="location-outline"></ion-icon>
        <div><strong>Registered Office</strong><br>20 Kirkdale Road, London, E11 1HP, United Kingdom</div>
      </div>
      <div class="info-contact-row">
        <ion-icon name="call-outline"></ion-icon>
        <div><strong>Phone</strong><br>
          <a href="tel:+442085560888">020 8556 0888</a>
        </div>
      </div>
      <div class="info-contact-row">
        <ion-icon name="mail-outline"></ion-icon>
        <div><strong>Email</strong><br>
          <a href="mailto:info@remitm.com">info@remitm.com</a>
        </div>
      </div>
      <div class="info-contact-row">
        <ion-icon name="globe-outline"></ion-icon>
        <div><strong>Website</strong><br>
          <a href="https://www.remitm.com">www.remitm.com</a>
        </div>
      </div>

      <h2>Get in Touch</h2>
      <p>We're here to help. Whether you have a question about a transfer, your account, or our services, reach out to us by phone or email and our team will be happy to assist.</p>
      <p>Please read our <a href="/privacy-policy">GDPR / Privacy Policy</a> information before submitting your details.</p>

      <p class="info-muted">Remitm Limited is a company registered in England &amp; Wales. Company Registration No: 07956213.</p>
    </div>
  </div></ion-content>`
})
export class ContactUsPage {}
