# SwiftBasket Infrastructure — Compose Instructions

The full infra stack (Kong gateway, MySQL, MongoDB, Redis, Kafka/ZooKeeper,
Elasticsearch) runs via containers; the six Spring Boot services run on the
host during development.

Two equivalent compose files are provided:

| File | Use with | Difference |
|---|---|---|
| `podman-compose.yml` | `podman-compose` | `host.docker.internal` mapped to the static podman-machine gateway IP |
| `docker-compose.yml` | `docker compose` | `host.docker.internal` mapped via `host-gateway` (Docker resolves it natively) |

## Podman

```sh
# Install podman-compose (if not already installed)
#   macOS: brew install podman-compose
#   Linux: pip install podman-compose

# Start the podman machine first (macOS/Windows only)
podman machine start

# Start all services
podman-compose -f infra/podman-compose.yml up -d

# View running containers
podman-compose -f infra/podman-compose.yml ps

# View logs
podman-compose -f infra/podman-compose.yml logs -f

# Stop all services
podman-compose -f infra/podman-compose.yml down

# Stop and delete volumes too (fresh databases)
podman-compose -f infra/podman-compose.yml down -v
```

## Docker

```sh
# Start all services
docker compose -f infra/docker-compose.yml up -d

# View running containers
docker compose -f infra/docker-compose.yml ps

# View logs
docker compose -f infra/docker-compose.yml logs -f

# Stop all services
docker compose -f infra/docker-compose.yml down

# Stop and delete volumes too (fresh databases)
docker compose -f infra/docker-compose.yml down -v
```

## Ports

| Container | Host port | Purpose |
|---|---|---|
| kong | 8000 (proxy), 8001 (admin) | API gateway — point all client calls at `:8000` |
| mysqldb | 3307 | user_db, product_db, order_db, payment_db, notification_db |
| mongodb | 27017 | cart_db |
| redis | 6379 | Cart cache |
| kafka | 9092 | Event broker |
| zookeeper | 2181 | Kafka coordination |
| elasticsearch | 9200 | product_index |
