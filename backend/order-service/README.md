# Order Service

Checkout, order history, delivery tracking, and admin fulfillment. It reacts to payment results over Kafka.

| | |
| --- | --- |
| Port | 8084 |
| Database | MySQL `order_db` (`orders`, `order_items`, `order_status_history`) |
| Publishes | `order.placed`, `order.status.updated` |
| Consumes | `payment.completed`, `payment.failed` |
| Calls | Cart service (read and clear the cart), product service (reserve and release stock) |

## Checkout

`POST /api/v1/orders` with a shipping address:
1. Reads the user's cart; an empty cart is rejected.
2. Reserves stock for each line, releasing earlier reservations if any line fails.
3. Saves the order as `PENDING` with item and price snapshots.
4. Publishes `order.placed`, which triggers payment and the confirmation email.
5. Clears the cart.

## Order statuses

```
PENDING -> PAID -> PROCESSING -> SHIPPED -> DELIVERED
   \          \
    +----------+-> CANCELLED          PENDING -> PAYMENT_FAILED (stock released)
```

`payment.completed` moves an order to `PAID`; `payment.failed` marks it `PAYMENT_FAILED` and returns its stock. Admins drive the remaining transitions. Each order response includes `allowedAdminStatuses`, and every change is recorded in the tracking history.

## Endpoints

| Method | Path | Access | Description |
| --- | --- | --- | --- |
| POST | `/api/v1/orders` | Signed in | Check out the current cart |
| GET | `/api/v1/orders` | Signed in | The caller's orders, newest first; `page`, `size` |
| GET | `/api/v1/orders/{id}` | Owner | Order details (`403` for other users' orders) |
| GET | `/api/v1/orders/{id}/tracking` | Owner | Status timeline |
| PATCH | `/api/v1/orders/{id}/status` | Admin | Apply an allowed status, with an optional note |
| GET | `/api/v1/orders/health` | Public | Liveness probe |

## Configuration

| Setting | Default |
| --- | --- |
| `spring.datasource.url` | `jdbc:mysql://localhost:3307/order_db` |
| `cart.service.url` / `product.service.url` | `http://localhost:8083` / `http://localhost:8082` |
| `swiftbasket.currency` | `INR` |

## Run

```sh
bash scripts/swiftbasket.sh start order         # from the repository root
# or
./mvnw spring-boot:run
```

API docs: http://localhost:8084/swagger-ui.html
