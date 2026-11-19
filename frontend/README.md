# SwiftBasket Storefront

Run `npm ci` and `npm run dev` from this directory. The Vite dev server proxies
`/api` to the local gateway on port 8000. Set `VITE_API_PROXY_TARGET` in the
environment that starts Vite to use a different gateway.

Catalog, cart, order, and payment data come from the backend. Run `npm run build`
for the production bundle and TypeScript check, or `npm run lint` for Oxlint.

## Test checkout

Set `STRIPE_TEST_PAYMENT_METHOD_ID=pm_card_visa` in the **payment service's**
environment and use a Stripe test API key there. Restart the payment service.
Checkout only offers the test method when the payment API returns it. The
storefront does not collect card details or support live card payments.
