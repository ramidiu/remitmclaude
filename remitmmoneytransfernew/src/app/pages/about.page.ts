import { Component } from '@angular/core';

@Component({
  selector: 'app-about',
  template: `
  <ion-content><div class="info-page">
    <div class="info-hero">
      <a class="info-back" href="/">&larr; Back to Home</a>
      <h1>About Us</h1>
      <p>Seamless global transfers you can trust</p>
    </div>
    <div class="info-body">
      <p>Our mission is to offer speedy, reliable and cost effective money transfer solutions to our customers in the UK to remit funds to family members, friends and associates abroad.</p>

      <h2>Vision</h2>
      <p>Our vision is to deliver a bespoke proposition to our clients through a highly professional service and a customer driven approach to our business.</p>

      <h2>Core Objectives</h2>
      <ul>
        <li>Grow the business by increasing its market share.</li>
        <li>Provide most reliable, efficient and secure services.</li>
        <li>Establish excellent relationships with our customers and other stakeholders based upon honesty, trust and respect.</li>
        <li>Employ effective policies, procedures, and systems to protect anti money laundering measures.</li>
        <li>Become a global well known household remittance brand.</li>
      </ul>

      <h2>Company Values</h2>
      <p>To continuously encourage and challenge our staff and to provide:</p>
      <div class="info-cards">
        <div class="info-card"><h3>Excellence in Service</h3><p class="info-muted">A highly professional, customer driven approach in everything we do.</p></div>
        <div class="info-card"><h3>Integrity</h3><p class="info-muted">Honesty, trust and respect at the heart of every relationship.</p></div>
        <div class="info-card"><h3>Innovation &amp; Growth</h3><p class="info-muted">Continually improving our branded and digital solutions.</p></div>
      </div>

      <h2>Our Team</h2>
      <p>We have a dedicated and diverse all round team who are empowered to deliver best value and service to our customers through our branded and digital solutions.</p>

      <p class="info-muted">Remitm Limited is a company registered in England &amp; Wales. Company Registration No: 07956213. Registered office: 20 Kirkdale Road, London, E11 1HP, United Kingdom.</p>
      <p><a href="/contact-us">Contact us &rarr;</a></p>
    </div>
  </div></ion-content>`
})
export class AboutPage {}
