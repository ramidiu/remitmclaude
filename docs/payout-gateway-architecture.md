# Payout Gateway Architecture

A configuration-driven payout layer. The customer UI and corridor logic never name a provider —
the active gateway (Nsano, Zeepay, Manual, or any future one) is resolved on the backend from the
corridor configuration. Switching a corridor's provider (e.g. **Ghana Mobile Wallet from Nsano to
Zeepay**) is a single admin toggle with **zero code changes**.

## Design principles

1. **`corridor_delivery_methods` is the single source of truth** for routing — it already maps
   `(corridor, delivery_method) → active payout_partner`. We did **not** add a parallel routing
   table (unlike the legacy `api_info`).
2. **A payout partner carries its gateway.** `payout_partners.gateway` = `NSANO | ZEEPAY | MANUAL | …`.
   The router resolves the partner, the partner tells us the gateway.
3. **The frontend is gateway-agnostic.** It calls a generic facade; the backend resolves + dispatches.
4. **Routing is stamped at creation.** `transactions.payout_gateway` is written when the transaction
   is created, so re-routing a corridor never affects in-flight transactions (auditable, immutable).
5. **Credentials live in a table** (`gateway_config`) so keys/tokens rotate without redeploy.

## Architecture

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                         CUSTOMER UI (gateway-agnostic)                              │
│  Add Recipient form  ─ types account/mobile ─►  validateRecipientGeneric()          │
│  Pay screen          ─ click "Pay"           ─►  disburse(ref)                       │
│   (never knows or names Nsano / Zeepay / Manual)                                    │
└───────────────────────────────────┬────────────────────────────────────────────────┘
                                     │  GET /api/payout/validate?receiveCurrency&deliveryMethod&account&bankOrProvider
                                     │  GET /api/payout/route?...
                                     │  POST /api/payout/disburse/{ref}
                                     ▼
┌──────────────────────────────────────────────────────────────────────────────────┐
│                        PayoutFacadeController   (generic API)                        │
│   /route  → returns { gateway, supportsNameCheck, async, partner }                  │
│   /validate → normalized { found, accountName }                                     │
│   /disburse → normalized { success, code, message }                                 │
└───────────────────────────────────┬────────────────────────────────────────────────┘
                                     │  resolve(receiveCurrency, deliveryMethod)
                                     ▼
┌──────────────────────────────────────────────────────────────────────────────────┐
│                         PayoutRoutingService  (the ROUTER)                           │
│                                                                                      │
│   receiveCurrency (GHS) + deliveryMethod (MOBILE_WALLET)                             │
│        │                                                                             │
│        ▼                                                                             │
│   corridors ──► corridor_delivery_methods ──► payout_partner ──► partner.gateway     │
│   (GBP→GHS)     (active row, this method)      (e.g. "Nsano")     ("NSANO")           │
│        └────────────── SINGLE SOURCE OF TRUTH (admin-toggle) ──────────────┘         │
└───────────────────────────────────┬────────────────────────────────────────────────┘
                                     │  registry.get("NSANO")
                                     ▼
┌──────────────────────────────────────────────────────────────────────────────────┐
│                      GatewayRegistry  (auto-discovers all gateways)                  │
│   Map<String, PayoutGateway>  { "NSANO":…, "ZEEPAY":…, "MANUAL":…, +future }         │
└───────┬───────────────────────────┬────────────────────────────┬────────────────────┘
        ▼                           ▼                            ▼
┌────────────────┐         ┌────────────────┐          ┌────────────────┐
│  NsanoGateway  │         │  ZeepayGateway │          │  ManualGateway │   ◄─ implement
│  validate()    │         │  validate()    │          │  (no live API) │      PayoutGateway
│  disburse()    │         │  disburse()    │          │                │
│  caps: name✓   │         │  caps: name✓   │          │  caps: name✗   │
└───────┬────────┘         └───────┬────────┘          └────────────────┘
        ▼                          ▼
   NsanoService              ZeepayService          ◄─ existing API integrations (unchanged)
   (Nsano API)               (Zeepay API)

                  ┌──────────────── future gateway ────────────────┐
                  │  ThunesGateway / FlutterwaveGateway / …         │
                  │  = 1 new class + gateway_config row + partner    │
                  │  NO change to UI, router, facade, or others.     │
                  └──────────────────────────────────────────────────┘
```

## Database (migration `V549__payout_gateway_abstraction.sql`)

| Change | Purpose |
|---|---|
| `payout_partners.gateway` | provider the partner disburses through (`NSANO`/`ZEEPAY`/`MANUAL`/…; null = MANUAL) |
| `transactions.payout_gateway` | the gateway resolved + **stamped at creation** (immutable route) |
| `gateway_config` (new table) | `gateway, base_url, api_key, api_token, token_expires_at, is_active` — credential rotation without redeploy |

## Code map

| Component | Class |
|---|---|
| Strategy interface | `payout/gateway/PayoutGateway.java` |
| Capabilities / DTOs | `GatewayCapabilities`, `ValidationRequest`, `ValidationResult`, `PayoutRoute` |
| Implementations | `payout/gateway/impl/{Nsano,Zeepay,Manual}Gateway.java` |
| Registry | `payout/gateway/GatewayRegistry.java` |
| Router | `payout/gateway/PayoutRoutingService.java` |
| Facade API | `payout/gateway/PayoutFacadeController.java` |
| Stamp on creation | `transaction/service/TransactionService.java` (`.payoutGateway(...)`) |
| Frontend facade calls | `core/services/beneficiary.service.ts` (`validateRecipientGeneric`, `getPayoutRoute`) |

## How to add a new gateway (e.g. Thunes)

1. **Write `ThunesGateway implements PayoutGateway`** — the one place Thunes' API is called
   (`validateRecipient()`, `disburse()`, `getCapabilities()`). Normalize its response into
   `ValidationResult` / the result map.
2. **Add a `gateway_config` row** with its base URL + key/token.
3. **Create a `payout_partner`** with `gateway = 'THUNES'`.
4. **Point the corridors at it** in `corridor_delivery_methods` (set the active payout partner).

That's it — **no changes** to the UI, router, facade, or any existing gateway.

## Switching a corridor's provider (the Ghana example)

`corridor_delivery_methods` for `GBP→GHS / MOBILE_WALLET` points at a payout partner. To move Ghana
mobile money from Nsano to Zeepay, change the active partner for that row to one whose `gateway =
'ZEEPAY'`. The customer's validation **and** payout immediately follow the new rail — no deploy.

## Future roadmap (designed-for, not yet built)

- **Capabilities-driven UI everywhere** — show the live verify / payout button only when the resolved
  gateway `supportsNameCheck` (partially wired: `validate` returns `supported`).
- **Credentials read from `gateway_config`** instead of env vars (the table exists; wiring pending).
  Encrypt at rest + track `token_expires_at` with alerting (fixes the expired-Zeepay-JWT class of bug).
- **Idempotency** at the gateway boundary keyed by transaction reference (must precede fallback).
- **Priority / fallback routing** in `corridor_delivery_methods` (primary + secondary), gated by a
  per-gateway **health/circuit-breaker** so failover is automatic. Note: fallback often needs
  **re-validation** (a recipient validated on gateway A may be invalid on B) or should fall back to
  `MANUAL` — do not build until a real second-provider-per-corridor need exists.
- **Partner-portal payout buttons** → route through `/api/payout/disburse` (currently still call the
  gateway-specific endpoints).
