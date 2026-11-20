# Payment Service

Stripe payments in **test mode**: one payment intent per order, server-side confirmation, receipts, and signed webhooks.

| | |
| --- | --- |
| Port | 8085 |
| Database | MySQL `payment_db` (`payments`, `payment_event_log`) |
| Consumes | `order.placed` |
| Publishes | `payment.completed`, `payment.failed` |
| Calls | Order service (amount, currency, owner), Stripe API |

## Flow

1. `order.placed`, or the checkout page calling the intent endpoint, creates a Stripe payment intent and a `REQUIRES_PAYMENT` record. A unique constraint keeps one payment per order, so concurrent requests return the existing payment.
2. The storefront confirms with the configured test payment method. Intents disallow redirect-based methods, so no return URL is needed.
3. On success the payment becomes `SUCCEEDED` and `payment.completed` is published; on failure it becomes `FAILED` and `payment.failed` is published.
4. The Stripe webhook keeps statuses in sync if confirmation results arrive later.

## Endpoints

| Method | Path | Access | Description |
| --- | --- | --- | --- |
| POST | `/api/v1/payments/orders/{orderId}/intent` | Owner | Create or return the order's payment intent |
| POST | `/api/v1/payments/orders/{orderId}/confirm` | Owner | Confirm with a `paymentMethodId` |
| GET | `/api/v1/payments/orders/{orderId}` | Owner | Payment status |
| GET | `/api/v1/payments/orders/{orderId}/receipt` | Owner | Receipt; `409` until the payment succeeds |
| POST | `/api/v1/payments/webhook/stripe` | Stripe | Verified with the `Stripe-Signature` header |
| GET | `/api/v1/payments/health` | Public | Liveness probe |

## Configuration

| Setting | Source | Notes |
| --- | --- | --- |
| `stripe.api-key` | `STRIPE_API_KEY` | Use a test key (`sk_test_…`) |
| `stripe.test-payment-method-id` | `STRIPE_TEST_PAYMENT_METHOD_ID` | For example `pm_card_visa`; returned to the storefront only with a test key |
| `stripe.webhook-secret` | `STRIPE_WEBHOOK_SECRET` | Only needed for webhook testing |
| `order.service.url` | `application.yaml` | `http://localhost:8084` |

The placeholder defaults let the service start, but payments fail until a real test key is set.

To test webhooks locally:

```sh
stripe listen --forward-to localhost:8000/api/v1/payments/webhook/stripe
```

Put the `whsec_…` value it prints in `STRIPE_WEBHOOK_SECRET` and restart the service.

## Run

```sh
bash scripts/swiftbasket.sh start payment       # from the repository root
# or, with the variables from scripts/.env.example exported
./mvnw spring-boot:run
```

API docs: http://localhost:8085/swagger-ui.html
