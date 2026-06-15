import { Component } from '@angular/core';

@Component({
  selector: 'app-cookie-policy',
  template: `
  <ion-content><div class="info-page">
    <div class="info-hero">
      <a class="info-back" href="/">&larr; Back to Home</a>
      <h1>Cookie Policy</h1>
      <p>Effective Date: 24 June 2025</p>
    </div>
    <div class="info-body">
      <p>We use cookies on the www.remitm.com website to help analyse web page flow, customise our service, content and advertising, measure promotional effectiveness, and enhance your user experience while promoting trust and safety.</p>

      <h2>What Are Cookies?</h2>
      <p>Cookies are small text files that are placed in an internet user's computer memory. The information the cookie contains is set by a website's server and helps the website to recognise your device the next time you visit.</p>
      <p>Cookies allow websites to respond to you as an individual. The website can tailor its operations to your needs, likes and dislikes by gathering and remembering information about your preferences. Without cookies, a website would not be able to keep track of what you did before and would treat you like a new visitor every time you browse to a different page.</p>
      <p>Cookies are not dangerous. They are one of the most basic web technologies and are used by most websites.</p>

      <h2>Registration and Login</h2>
      <p>Actions such as registration and login require cookies. If you register or log in to the site, you will override your preference not to accept cookies.</p>

      <h2>How We Use Cookies</h2>
      <ul>
        <li>To analyse web page flow and how the site is used.</li>
        <li>To customise our service, content and advertising.</li>
        <li>To measure promotional effectiveness.</li>
        <li>To enhance your user experience and promote trust and safety.</li>
      </ul>

      <h2>Sharing with Social Networks</h2>
      <p>Some pages may include features from social networks that can set their own cookies. These are governed by the privacy policies of those providers.</p>

      <h2>Opting Out of Cookies</h2>
      <p>You can control or delete cookies through your browser settings. Please note that rejecting or deleting certain cookies may affect functionality such as registration and login. For additional information you may wish to read our Privacy Policy or visit www.aboutcookies.org.</p>

      <h2>Contact</h2>
      <p>Questions about cookies? Email <a href="mailto:info@remitm.com">info@remitm.com</a>. See also our <a href="/privacy-policy">Privacy Policy</a>.</p>
      <p class="info-muted">Remitm Limited is a company registered in England &amp; Wales. Company Registration No: 07956213.</p>
    </div>
  </div></ion-content>`
})
export class CookiePolicyPage {}
