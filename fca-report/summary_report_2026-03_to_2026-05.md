# FCA Transaction Report — Reporting Period: 01 Mar 2026 – 31 May 2026

> **Generated from the Remitm application database (`remitm`) on 20 Jun 2026.** All values in
> **GBP equivalent** unless stated. Foreign-currency legs converted at the transaction's applied
> FX rate. Your Compliance Officer must confirm this maps to the correct FCA RegData return for
> the firm's permissions before submission.
>
> ⚠️ **DATA-SOURCE CAVEAT — read first.** The application database holds **only 8 transactions
> for this period** (Mar 0, Apr 4, May 4), all from a **single migrated legacy customer** to
> Ghana, totalling **£680.50**. This profiles as residual legacy/low-value activity, **not** a
> complete record of live trading. If the firm processed other customer payments in Mar–May 2026,
> those were handled outside this database and must be sourced separately before this return is
> treated as complete. The figures below are accurate **for what this system holds**.

## A. Firm & report details
| Field | Value |
|---|---|
| Firm name | Remitm Limited |
| FCA Firm Reference Number (FRN) | _____________ |
| Permissions / category | Payment Institution (money remittance) — confirm |
| Reporting period | 01 Mar 2026 – 31 May 2026 |
| Currency basis | GBP equivalent |
| Prepared by | _____________ |
| Date prepared | 20 Jun 2026 |
| Reviewed/approved by (MLRO/Compliance) | _____________ |

## B. Headline figures
| Metric | Value |
|---|---|
| Total transactions (count) | 8 |
| Total value (GBP) | 680.50 |
| Average transaction value (GBP) | 85.06 |
| Median transaction value (GBP) | 26.50 |
| Largest single transaction (GBP) | 308.50 |
| Active sending customers | 1 |
| New customers onboarded in period | 0 |
| Total fees / firm income (GBP) | 0.00 |

## C. Volume & value by month
| Month | Transactions | Value (GBP) | Avg (GBP) | Active customers |
|---|---|---|---|---|
| Mar 2026 | 0 | 0.00 | — | 0 |
| Apr 2026 | 4 | 468.50 | 117.13 | 1 |
| May 2026 | 4 | 212.00 | 53.00 | 1 |
| **Total** | **8** | **680.50** | **85.06** | **1** |

## D. By destination country / corridor
| Destination country | Corridor | Transactions | Value (GBP) | % of value | High-risk jurisdiction (Y/N) |
|---|---|---|---|---|---|
| Ghana (GH) | GBP→GHS | 8 | 680.50 | 100% | N |

## E. By currency (receive)
| Receive currency | Transactions | Value (GBP) |
|---|---|---|
| GHS | 8 | 680.50 |

## F. By payout method
| Payout method | Transactions | Value (GBP) | % of value |
|---|---|---|---|
| Bank deposit | 0 | 0.00 | 0% |
| Mobile wallet | 8 | 680.50 | 100% |
| Cash pickup | 0 | 0.00 | 0% |

## G. By status
| Status | Transactions | Value (GBP) |
|---|---|---|
| Completed / Paid | 8 | 680.50 |
| Pending | 0 | 0.00 |
| Cancelled | 0 | 0.00 |
| Failed | 0 | 0.00 |
| Refunded | 0 | 0.00 |

## H. Transaction-size distribution (AML)
| Size band (GBP) | Transactions | Value (GBP) |
|---|---|---|
| < 100 | 5 | 97.00 |
| 100 – 999.99 | 3 | 583.50 |
| 1,000 – 9,999.99 | 0 | 0.00 |
| ≥ 10,000 | 0 | 0.00 |

## I. Large / threshold transactions (Enhanced Due Diligence)
_List every transaction ≥ £10,000 or ≥ €15,000, plus any aggregated/linked transfers that
breach the threshold in aggregate._

**None.** No single transaction reached £10,000 / €15,000. Largest was £308.50. Aggregate
activity by the one active customer over the full period was £680.50 — well below threshold.

## J. High-risk jurisdiction exposure
| Jurisdiction (FATF high-risk / sanctioned) | Transactions | Value (GBP) | % of total value |
|---|---|---|---|
| _None_ | 0 | 0.00 | 0% |
| **Total high-risk** | **0** | **0.00** | **0%** |

> Sole destination was Ghana, which is **not** currently on the FATF "high-risk" (black) or
> "increased monitoring" (grey) lists. Confirm against the live FATF/HM Treasury lists at
> submission date.

## K. Customer metrics
| Metric | Value |
|---|---|
| Total active senders | 1 |
| New customers onboarded | 0 |
| Avg transactions per customer | 8.0 |
| KYC tier mix (TIER_1 / TIER_2 / TIER_3) | 0 / 1 / 0 |
| Customers in high-risk countries | 0 |

> The one active sender (Kingsley Odame-Danquah, GB resident, KYC TIER_2, status ACTIVE) is a
> migrated legacy customer onboarded 13 Dec 2022.

## L. Firm income
| Metric | Value (GBP) |
|---|---|
| Total fees charged | 0.00 |
| FX margin income (if applicable) | 0.00 |
| Total income | 0.00 |

> No fee or FX-margin amounts are recorded against these transactions. Confirm whether income
> was taken at a different layer (e.g. the FX spread embedded in the 14.60 GBP→GHS rate).

## M. Financial-crime summary
| Metric | Value |
|---|---|
| SARs filed in period | 0 |
| Transactions flagged / under review | 0 |
| EDD cases completed | 0 (none required — no threshold breach) |
| Refunds / reversals (count, value) | 0 (£0.00) |
| Declined / blocked (sanctions, fraud) | 0 |

> All 8 transactions carry an automated risk score of 0 and completed to PAID. No SAR, hold, or
> reversal recorded. Confirm PEP/sanctions screening was performed and evidenced for the sender.

## N. Purpose of transfer & source of funds
Captured at migration and stored in the transaction `notes` field (the live schema has no
dedicated columns for these). All 8 transactions:
| Purpose / reason for transfer | Source of funds | Transactions | Value (GBP) |
|---|---|---|---|
| Family or Friend Support | Savings | 8 | 680.50 |

## O. Data gaps (not held in this system)
The following FCA/AML-relevant fields are **not captured** in the current records and must be
sourced from KYC/compliance files if required by the chosen return:
- Sender nationality (held as NULL)
- PEP / sanctions screening result & evidence
- SAR references (if any filed outside the system)

## P. Declaration
> I confirm the figures in this report are, to the best of my knowledge, complete and accurate
> and reconcile to the firm's transaction records for the period **held in the `remitm`
> system**, subject to the data-source caveat above.

| Name | Role | Signature | Date |
|---|---|---|---|
|  | MLRO / Compliance Officer |  |  |
