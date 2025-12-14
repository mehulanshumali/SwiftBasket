**How to Use with Podman:**
---------------------------
```sh
# Install podman-compose (if not already installed)
# On macOS: brew install podman-compose
# On Linux: pip install podman-compose

# Start all services
podman-compose -f infra/podman-compose.yml up -d

# View running containers
podman-compose -f infra/podman-compose.yml ps

# View logs
podman-compose -f infra/podman-compose.yml logs -f

# Stop all services
podman-compose -f infra/podman-compose.yml down

# Clean up volumes too
podman-compose -f infra/podman-compose.yml down -v
```

**Important Notes for Podman:**
-------------------------------
1.  **Podman Machine (macOS/Windows)**: If using Podman Desktop, ensure the machine is running:
```sh
podman machine start
```
2.  **Rootless Podman**: If running rootless Podman, volume permissions might need adjustment:
```sh
podman unshare chown -R 100:101 ./infra
```
3.  **Network Access**: Container-to-container networking works via service names (e.g., `kafka:9092` instead of `localhost:9092` from inside containers)   
4.  **Localhost Access**: From your machine, services are accessible at `localhost:PORT` as shown in the ports