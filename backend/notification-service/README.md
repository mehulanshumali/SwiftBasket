# Notification Service

Sends emails in response to Kafka events and keeps an audit record of every notification.

| | |
| --- | --- |
| Port | 8086 |
| Database | MySQL `notification_db` (`notifications`) |
| Consumes | `user.registered`, `order.placed`, `payment.completed`, `order.status.updated` |
| Calls | User service, to resolve the recipient's email |
| Email | Mailtrap SMTP |

## Notifications

| Event | Type | Email |
| --- | --- | --- |
| `user.registered` | `WELCOME` | Welcome to SwiftBasket |
| `order.placed` | `ORDER_CONFIRMATION` | Order #N confirmed |
| `payment.completed` | `PAYMENT_RECEIPT` | Payment receipt for order #N |
| `order.status.updated` | `ORDER_STATUS_UPDATE` | Order #N is now STATUS |

Each notification is saved as `PENDING`, then marked `SENT` or `FAILED`. Events carry a user ID rather than an email address; the service looks up the current address from the user service and caches it for 15 minutes.

Emails are sent one at a time and retried on Mailtrap's rate-limit error, because the free plan rejects bursts with `550 Too many emails per second`.

## Endpoints

| Method | Path | Access | Description |
| --- | --- | --- | --- |
| GET | `/api/v1/notifications` | Signed in | The caller's notifications, newest first; `page`, `size` |
| GET | `/api/v1/notifications/health` | Public | Liveness probe |

## Configuration

| Setting | Source | Default |
| --- | --- | --- |
| `spring.mail.username` / `password` | `MAILTRAP_USERNAME` / `MAILTRAP_PASSWORD` | Dummy values; sends fail until set |
| `user.service.url` | `application.yaml` | `http://localhost:8081` |

## Run

```sh
bash scripts/swiftbasket.sh start notification  # from the repository root
# or, with the variables from scripts/.env.example exported
./mvnw spring-boot:run
```

API docs: http://localhost:8086/swagger-ui.html
