# Cart Service

Shopping carts for guests and signed-in users, stored in MongoDB with a Redis read cache.

| | |
| --- | --- |
| Port | 8083 |
| Storage | MongoDB `cart_db`; Redis cache (24-hour TTL, evicted on every change) |
| Publishes | `cart.item.added` |
| Calls | Product service, to validate products and current prices |

## Cart identity

A request identifies its cart by:
- `X-User-ID`, set by Kong for signed-in users, or
- `X-Session-ID`, a random ID the storefront generates for guests.

On login, `POST /api/v1/cart/merge` with both headers moves the guest cart's items into the user's cart.

## Endpoints

| Method | Path | Description |
| --- | --- | --- |
| GET | `/api/v1/cart` | Current cart with line totals, total, and currency |
| POST | `/api/v1/cart/items` | Add a product (`productId`, `quantity`) |
| PATCH | `/api/v1/cart/items/{productId}?quantity=` | Change a line's quantity |
| DELETE | `/api/v1/cart/items/{productId}` | Remove a line |
| DELETE | `/api/v1/cart` | Clear the cart |
| POST | `/api/v1/cart/merge` | Merge the guest cart into the user cart |
| GET | `/api/v1/cart/health` | Liveness probe |

## Configuration

| Setting | Default |
| --- | --- |
| `spring.data.mongodb.uri` | `mongodb://localhost:27017/cart_db` |
| `spring.data.redis.host` / `port` | `localhost` / `6379` |
| `product.service.url` | `http://localhost:8082`; required at startup |
| `swiftbasket.currency` | `INR` |

## Run

```sh
bash scripts/swiftbasket.sh start cart          # from the repository root
# or
./mvnw spring-boot:run
```

API docs: http://localhost:8083/swagger-ui.html
