# Product Service

The catalog: categories, products, stock, and full-text search. MySQL is the source of truth and Elasticsearch serves search.

| | |
| --- | --- |
| Port | 8082 |
| Database | MySQL `product_db` (`categories`, `products`) |
| Search | Elasticsearch index `product_index` |
| Events | None |

## Endpoints

| Method | Path | Access | Description |
| --- | --- | --- | --- |
| GET | `/api/v1/products` | Public | Paged active products; `categoryId`, `page`, `size` |
| GET | `/api/v1/products/search` | Public | Fuzzy full-text search; `q`, `page`, `size` |
| GET | `/api/v1/products/{id}` | Public | Product details, price, stock |
| POST | `/api/v1/products` | Admin | Create a product (unique SKU) |
| PUT | `/api/v1/products/{id}` | Admin | Update and reindex a product |
| DELETE | `/api/v1/products/{id}` | Admin | Soft-delete and remove from search |
| POST | `/api/v1/products/reindex` | Admin | Rebuild the search index from MySQL |
| POST | `/api/v1/products/{id}/stock` | Internal | Reserve (`delta` < 0) or release (`delta` > 0) stock; used by the order service |
| GET | `/api/v1/categories` | Public | All categories |
| POST, PUT, DELETE | `/api/v1/categories[/{id}]` | Admin | Manage categories |
| GET | `/api/v1/products/health` | Public | Liveness probe |

Admin routes check the `X-User-Role` header set by Kong. Stock changes use optimistic locking, so a conflicting concurrent update returns `409`.

## Configuration

| Setting | Default |
| --- | --- |
| `spring.datasource.url` | `jdbc:mysql://localhost:3307/product_db` |
| `spring.elasticsearch.uris` | `http://localhost:9200` |
| `swiftbasket.currency` | `INR`, returned with prices |

If search results look stale or empty after loading data directly into MySQL, call the reindex endpoint.

## Run

```sh
bash scripts/swiftbasket.sh start product       # from the repository root
# or
./mvnw spring-boot:run
```

API docs: http://localhost:8082/swagger-ui.html
