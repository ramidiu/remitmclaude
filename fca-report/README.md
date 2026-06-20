# FCA Transaction Report — Templates (Mar–May 2026)

Industry-standard templates for a UK money-remittance firm (Payment Institution under the
Payment Services Regulations 2017). Use these to evidence the FCA's periodic returns
(RegData), e.g. the payment-services data item, REP-CRIM (financial crime), and PSD2 fraud
reporting. The FCA's focus is AML / financial-crime oversight and accuracy of reported volumes.

## Files
| File | Purpose |
|---|---|
| `transaction_extract_template.csv` | **Line-item audit trail** — one row per transaction. Header only; rows appended from the system-of-record. |
| `summary_report_template.md` | **Summary report** — headline figures + all FCA/AML breakdowns (by month, country/corridor, currency, payout method, status, size band, large/threshold txns, high-risk jurisdictions, customers, income, financial-crime). Blank cells to populate. |

## How to use
1. **Confirm the exact FCA return** with your Compliance Officer/MLRO — the precise RegData
   item depends on the firm's permissions and reporting schedule. Map these fields to it.
2. **Confirm the data source** for 01 Mar–31 May 2026 (see caveat below).
3. Populate `transaction_extract_template.csv` with every transaction in the period.
4. Derive the `summary_report_template.md` figures from that extract.
5. Have the MLRO/Compliance Officer review, sign the declaration, and submit.

## ⚠️ Data-source caveat (important)
The current application database (`remitm`) holds only **~8 transactions for Mar–May 2026**
(Mar 0, Apr 4, May 4), and the wider dataset profiles as legacy/test data (avg ~£40, max £750,
some £0.01 amounts). **It is not a reliable system-of-record for an FCA return.** The real
production transactions for the period must be sourced from wherever live customer payments
were actually processed. Once that source is confirmed, the extract + summary can be generated
automatically.

## Field reference (transaction extract)
Reference · Date Created · Date Completed · Status · Sender (ID/Name/Email/Country/KYC tier &
status) · Beneficiary (Name/Country) · High-risk flag · Send amount/currency · GBP equivalent ·
FX rate · Receive amount/currency · Fee · Total debit · Payout method · Payout gateway ·
Funding method · Purpose · Source of funds · Size band · Threshold flag (≥£10k / ≥€15k) ·
SAR filed · Refunded/Reversed · Notes.

> Not regulatory/legal advice — a working template. Final responsibility for completeness,
> accuracy, and choice of FCA return rests with the firm's compliance function.
