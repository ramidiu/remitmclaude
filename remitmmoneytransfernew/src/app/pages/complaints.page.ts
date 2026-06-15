import { Component } from '@angular/core';

@Component({
  selector: 'app-complaints',
  template: `
  <ion-content><div class="info-page">
    <div class="info-hero">
      <a class="info-back" href="/">&larr; Back to Home</a>
      <h1>Complaint Policy</h1>
      <p>We take complaints seriously and aim to resolve them fairly and quickly</p>
    </div>
    <div class="info-body">
      <p>Remitm Limited treats all complaints seriously, as it is an important way to improve our money transfer services to customers. Our Complaints Handling Procedure is the process for addressing issues that arise when customers feel their expectations are not met. We operate in accordance with the Financial Conduct Authority (FCA) and the Financial Ombudsman Service (FOS) complaints management procedure.</p>
      <p>We recognise that we have an obligation to all customers who are dissatisfied with our service to resolve any complaint from the point of notification. If this is not possible for any reason, then we will state our reasons for not being able to do so.</p>

      <h2>How to Make a Complaint</h2>
      <p>Complaints can be lodged via the following channels:</p>
      <ul>
        <li>Telephone: <a href="tel:+442085560888">0208 556 0888</a></li>
        <li>Email: <a href="mailto:complaints@remitm.com">complaints@remitm.com</a></li>
        <li>Online: <a href="https://www.remitm.com/complaints">www.remitm.com/complaints</a></li>
        <li>By Post: Remitm Limited, 20 Kirkdale Road, London, E11 1HP</li>
      </ul>
      <p>Please provide your name, contact details and an outline of the complaint so we can investigate it. Every complaint received is logged and given a unique reference number; please quote this number on any follow-up contact.</p>

      <h2>How We Handle Complaints</h2>
      <ul>
        <li><strong>Immediately</strong> — We will work to resolve your complaint as quickly as possible and send a Summary Resolution Communication for complaints resolved within 3 business days of receipt.</li>
        <li><strong>5 business days</strong> — If we cannot resolve it straight away, we will provide a formal acknowledgement within 5 business days, along with an estimated timeframe for resolution.</li>
        <li><strong>15 business days</strong> — In most cases we will resolve your complaint within 15 business days, providing the results of our investigation and decision in a written final response.</li>
        <li><strong>35 business days</strong> — In exceptional circumstances, we will provide our final response no later than 35 business days after receipt of your complaint.</li>
      </ul>

      <h2>If You're Not Satisfied</h2>
      <p>If you are dissatisfied with our final resolution, or if over 35 business days have passed since you first raised your complaint, you may have the right to refer your complaint to the <strong>Financial Ombudsman Service (FOS)</strong>. The FOS will only deal with your complaint once you have tried to resolve it with us first.</p>
      <p>If you would like the FOS to look into your complaint, you must contact them within six months of the date of our final response:</p>
      <ul>
        <li>The Financial Ombudsman Service, Exchange Tower, Harbour Exchange Square, London E14 9SR</li>
        <li>Phone: 0800 023 4567</li>
        <li>Email: <a href="mailto:complaint.info@financial-ombudsman.org.uk">complaint.info@financial-ombudsman.org.uk</a></li>
        <li>Website: <a href="https://www.financial-ombudsman.org.uk" target="_blank" rel="noopener">www.financial-ombudsman.org.uk</a></li>
      </ul>

      <p class="info-muted">Remitm Limited is a company registered in England &amp; Wales. Company Registration No: 07956213. Registered office: 20 Kirkdale Road, London, E11 1HP, United Kingdom.</p>
    </div>
  </div></ion-content>`
})
export class ComplaintsPage {}
