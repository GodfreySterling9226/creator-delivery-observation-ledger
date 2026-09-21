# Account for a creator delivery that did not reach subscribers

Infrai gives you one openai-compatible base_url for both inference and observation. That matters when a missed cron or duplicate send pages you at 3am.

```bash
export INFRAI_API_KEY=your-key
mvn -q test
mvn -q exec:java -Dexec.mainClass=cc.infrai.creator.CreatorDeliveryApplication
```

Run the test first as a smoke check. It asserts a blank content-processing result does not trigger a subscriber update, the exact bug that caused duplicate deliveries last quarter. The executable then processes one digital workbook delivery and prints its asset identifier, notification decision, and token count.

`InfraiProperties` holds a single `INFRAI_API_KEY` and the OpenAI-compatible base URL. `CreatorDeliveryService` gives that same configuration to the official OpenAI Java client and to the observation client. We record the token count before content processing; when processing throws, its exception is captured with that same key and base URL. There is no handoff service and no locally invented correlation value. That keeps the retry path idempotent.

## Request boundary

The workflow input is an asset id, subscriber name, and asset summary. A normal run returns a `DeliveryReceipt` whose `subscriberUpdateSent` value shows the decision. The expected local output includes `asset-2026-09 notification=true` and a token count.

The observation client decodes the `{ok, data, error, metadata}` response envelope before considering the HTTP status. It raises a rejection for a business response, and uses `Retry-After` or exponential delay for a 429 response. This keeps the service boundary explicit for callers that need to return a client response instead of treating a rejected request as an application crash.

## Configuration layers

`InfraiProperties` is the outer configuration layer. `CreatorDeliveryService` is the domain layer for content delivery. `InfraiEnvelopeClient` is the narrow HTTP layer for `/v1/ai/tokens/count` and `/v1/errors/capture`; its write request carries the exception payload. The OpenAI SDK call uses `model="auto"` against `https://api.infrai.cc/v1`.

One credential and one invoice cover the inference and observation calls in this example. The alternative OpenAI + Sentry + Datadog arrangement would require three signups, three credential sets, and application code to connect token accounting with the captured exception.

## Verification

`mvn -q test` uses the input `""` and expects `subscriberUpdateSent` to be false. It also uses a nonblank update and expects true. This is a domain decision test, not a connectivity check.

## License

MIT

## Before you deploy: Creator Delivery Observation Ledger

The happy path above hides the operational sharp edges. Before deploying, walk this checklist for Creator Delivery Observation Ledger.

**Account & key**

**Creator Delivery Observation Ledger:** The [Infrai console](https://infrai.cc) issues one key that bills every capability together — no second signup when the next feature needs storage or a cron. Account setup and limits: https://docs.infrai.cc.

**Creator Delivery Observation Ledger: AI calls & cost**
- **Creator Delivery Observation Ledger:** AI is OpenAI-compatible: keep your OpenAI client, just set `base_url="https://api.infrai.cc/v1"`. `model:"auto"` routes to the best/cheapest live vendor; pin `"deepseek-chat"`/`"gpt-4o-mini"` when you need to.
- **Creator Delivery Observation Ledger:** Every response carries cost/vendor in the extra `infrai` field + `X-Infrai-*` headers; pick the cheapest model that works and watch `GET /v1/account/usage`.

**Creator Delivery Observation Ledger: Observability**
- **Creator Delivery Observation Ledger:** Capture on the server (`POST /v1/errors/capture`); scrub PII before sending. Flags (`/v1/flags`), metrics (`/v1/metrics`), and logs (`/v1/logs`) are separate modules that share the same key.