import { Component } from '@angular/core';

@Component({
  selector: 'app-cash-pick-locations',
  template: `
  <ion-content><div class="info-page">
    <div class="info-hero">
      <a class="info-back" href="/">&larr; Back to Home</a>
      <h1>Cash Pick Locations</h1>
      <p>Collect your money in cash at our partner payout locations</p>
    </div>
    <div class="info-body">
      <p>With RemitM, your recipients can collect funds in cash at thousands of trusted partner payout locations across the countries we serve. Cash pickup is a fast and convenient way to send money to family, friends and associates who may not have a bank account.</p>

      <h2>Where you can collect cash</h2>
      <p>Cash pickup is available through our network of payout partners in supported countries, including Ghana, Nigeria and other destinations across our coverage. Partner locations include banks, agent branches and authorised cash payout points.</p>
      <ul>
        <li>Ghana &mdash; partner branches and agent locations nationwide.</li>
        <li>Nigeria &mdash; partner branches and agent locations nationwide.</li>
        <li>Other supported countries &mdash; contact us to confirm availability in your destination.</li>
      </ul>

      <h2>How it works</h2>
      <ul>
        <li>Choose &ldquo;cash pickup&rdquo; as the payout method when you send your transfer.</li>
        <li>Share the reference number with your recipient once the transfer is ready.</li>
        <li>Your recipient visits a partner payout location with valid photo identification and the reference number to collect the cash.</li>
      </ul>

      <h2>Confirming your pickup point</h2>
      <p>The exact pickup point available to your recipient is confirmed at the time of payout, as our partner network is regularly updated to give you the widest possible coverage. If you would like to confirm whether cash pickup is available at a specific location, please get in touch with our support team before you send.</p>

      <p class="info-muted">Remitm Limited is a company registered in England &amp; Wales. Company Registration No: 07956213. Registered office: 20 Kirkdale Road, London, E11 1HP, United Kingdom. Tel: 020 8556 0888. E-mail: info@remitm.com. Website: www.remitm.com.</p>
      <p><a href="/contact-us">Contact us &rarr;</a></p>
    </div>
  </div></ion-content>`
})
export class CashPickLocationsPage {}
