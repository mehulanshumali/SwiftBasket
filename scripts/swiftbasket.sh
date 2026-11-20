#!/usr/bin/env bash
#
# SwiftBasket service manager - controls the infra containers (podman-compose)
# and the six Spring Boot services (start/stop/restart/status/logs).
#
# Usage:
#   scripts/swiftbasket.sh start   [service|infra|all]   start service(s) in dependency order
#   scripts/swiftbasket.sh stop    [service|infra|all]   stop service(s) (reverse order)
#   scripts/swiftbasket.sh restart [service]             restart a single service
#   scripts/swiftbasket.sh status                       health-check table for all services
#   scripts/swiftbasket.sh logs <service>                tail a service's log (Ctrl-C to exit)
#
# Services: user product cart order payment notification
# Optional: source scripts/.env (copied from scripts/.env.example) for MAILTRAP_*/STRIPE_*/JWT_SECRET env vars.

set -u

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LOG_DIR="$ROOT_DIR/logs"
PID_DIR="$ROOT_DIR/logs"
ENV_FILE="$ROOT_DIR/scripts/.env"
COMPOSE_FILE="$ROOT_DIR/infra/podman-compose.yml"

# dependency order for startup (reverse for shutdown)
SERVICE_ORDER=(user product cart order payment notification)
GATEWAY_PORT=8000
HEALTH_TIMEOUT=90

port_of() {
    case "$1" in
        user)         echo 8081 ;;
        product)      echo 8082 ;;
        cart)         echo 8083 ;;
        order)        echo 8084 ;;
        payment)      echo 8085 ;;
        notification) echo 8086 ;;
    esac
}

# each service exposes its own health route under its controller path
health_path_of() {
    case "$1" in
        user)         echo "/api/v1/auth/health" ;;
        product)      echo "/api/v1/products/health" ;;
        cart)         echo "/api/v1/cart/health" ;;
        order)        echo "/api/v1/orders/health" ;;
        payment)      echo "/api/v1/payments/health" ;;
        notification) echo "/api/v1/notifications/health" ;;
    esac
}

RED=$'\033[0;31m'; GREEN=$'\033[0;32m'; YELLOW=$'\033[1;33m'; CYAN=$'\033[0;36m'; NC=$'\033[0m'

mkdir -p "$LOG_DIR" "$PID_DIR"

# ---------------------------------------------------------------- helpers ---

log()  { printf "${CYAN}[swiftbasket]${NC} %s\n" "$*"; }
ok()   { printf "${GREEN}[ok]${NC} %s\n" "$*"; }
warn() { printf "${YELLOW}[warn]${NC} %s\n" "$*"; }
err()  { printf "${RED}[error]${NC} %s\n" "$*" >&2; }

is_valid_service() {
    local s
    for s in "${SERVICE_ORDER[@]}"; do [[ "$s" == "$1" ]] && return 0; done
    return 1
}

pid_of() {
    local pidfile="$PID_DIR/$1.pid"
    [[ -f "$pidfile" ]] || return 1
    local pid
    pid="$(cat "$pidfile" 2>/dev/null)"
    [[ -n "$pid" ]] && kill -0 "$pid" 2>/dev/null
}

health_ok() {
    local service="$1" port="$2" path
    path="$(health_path_of "$service")"
    curl -sf -o /dev/null --max-time 3 "http://localhost:$port$path"
}

wait_healthy() {
    # $1 = service name, $2 = port
    local service="$1" port="$2" elapsed=0
    log "Waiting for $service on port $port (up to ${HEALTH_TIMEOUT}s)..."
    while (( elapsed < HEALTH_TIMEOUT )); do
        if health_ok "$service" "$port"; then
            ok "$service is healthy (took ${elapsed}s)"
            return 0
        fi
        if ! pid_of "$service"; then
            err "$service process died during startup - check $LOG_DIR/$service.log"
            tail -20 "$LOG_DIR/$service.log" 2>/dev/null
            return 1
        fi
        sleep 2; elapsed=$(( elapsed + 2 ))
    done
    err "$service did not become healthy within ${HEALTH_TIMEOUT}s - check $LOG_DIR/$service.log"
    return 1
}

# ---------------------------------------------------------------- infra -----

infra_up() {
    log "Starting infra containers (podman-compose)..."
    ( cd "$ROOT_DIR" && podman-compose -f "$COMPOSE_FILE" up -d ) || { err "podman-compose up failed"; return 1; }
    ok "Infra containers started"
}

infra_down() {
    log "Stopping infra containers..."
    ( cd "$ROOT_DIR" && podman-compose -f "$COMPOSE_FILE" down ) || { err "podman-compose down failed"; return 1; }
    ok "Infra containers stopped"
}

infra_running() {
    podman ps --format '{{.Names}}' 2>/dev/null | grep -q swiftbasket-
}

# ---------------------------------------------------------------- services ---

port_in_use() {
    lsof -nP -iTCP:"$1" -sTCP:LISTEN >/dev/null 2>&1
}

start_service() {
    local service="$1" dir="$ROOT_DIR/backend/$1-service"
    if pid_of "$service"; then
        warn "$service is already running (pid $(cat "$PID_DIR/$service.pid"))"
        return 0
    fi
    local port
    port="$(port_of "$service")"
    if port_in_use "$port"; then
        err "$service cannot start - port $port is already in use by another process (not managed by this script). Stop it or run 'scripts/swiftbasket.sh stop $service'."
        return 1
    fi
    log "Starting $service..."
    ( cd "$dir" && nohup mvn -q spring-boot:run > "$LOG_DIR/$service.log" 2>&1 < /dev/null &
      echo $! > "$PID_DIR/$service.pid" )
    sleep 1
    if ! pid_of "$service"; then
        err "$service failed to launch - tail of $LOG_DIR/$service.log:"
        tail -10 "$LOG_DIR/$service.log" 2>/dev/null
        return 1
    fi
    ok "$service launching (pid $(cat "$PID_DIR/$service.pid")), log: logs/$service.log"
}

# kill the mvn wrapper AND any forked java child still holding the service port
kill_service_tree() {
    # $1 = pid from pidfile (mvn wrapper), $2 = service port
    kill "$1" 2>/dev/null
    local waited=0
    while kill -0 "$1" 2>/dev/null && (( waited < 15 )); do
        sleep 1; waited=$(( waited + 1 ))
    done
    # mvn spawns java as a child; if it survived, kill whatever holds the port
    if port_in_use "$2"; then
        local child
        child="$(lsof -t -nP -iTCP:"$2" -sTCP:LISTEN | head -1)"
        if [[ -n "$child" ]]; then
            warn "child process $child still holds port $2 - killing it"
            kill "$child" 2>/dev/null
            waited=0
            while kill -0 "$child" 2>/dev/null && (( waited < 10 )); do
                sleep 1; waited=$(( waited + 1 ))
            done
            kill -9 "$child" 2>/dev/null
        fi
    fi
}

stop_service() {
    local service="$1" port
    port="$(port_of "$service")"
    if ! pid_of "$service"; then
        rm -f "$PID_DIR/$service.pid"
        # pidfile stale/missing, but something still holds the port - stop it anyway
        if port_in_use "$port"; then
            warn "$service has no pidfile but port $port is occupied - killing the process on it"
            local pid
            pid="$(lsof -t -nP -iTCP:"$port" -sTCP:LISTEN | head -1)"
            if [[ -n "$pid" ]]; then
                kill "$pid" 2>/dev/null
                local waited=0
                while kill -0 "$pid" 2>/dev/null && (( waited < 20 )); do
                    sleep 1; waited=$(( waited + 1 ))
                done
                kill -9 "$pid" 2>/dev/null
                ok "$service stopped (pid $pid)"
                return 0
            fi
        fi
        warn "$service is not running"
        return 0
    fi
    local pid
    pid="$(cat "$PID_DIR/$service.pid")"
    log "Stopping $service (pid $pid)..."
    kill_service_tree "$pid" "$port"
    rm -f "$PID_DIR/$service.pid"
    ok "$service stopped"
}

cmd_start() {
    local targets=("${@:-all}")
    # shellcheck disable=SC1090
    [[ -f "$ENV_FILE" ]] && set -a && source "$ENV_FILE" && set +a && log "Loaded env from scripts/.env"

    local t
    for t in "$@"; do
        if [[ "$t" == "infra" ]]; then
            infra_up || return 1
        elif [[ "$t" == "all" ]]; then
            :
        elif ! is_valid_service "$t"; then
            err "Unknown service '$t' - valid: ${SERVICE_ORDER[*]} | infra | all"
            return 1
        fi
    done

    # decide what to start
    local to_start=()
    if [[ $# -eq 0 ]] || [[ "${1:-}" == "all" ]]; then
        to_start=("${SERVICE_ORDER[@]}")
    else
        for t in "$@"; do is_valid_service "$t" && to_start+=("$t"); done
    fi

    # any Java service needs the infra containers (MySQL/Kafka/Redis/Mongo/ES)
    if (( ${#to_start[@]} > 0 )); then
        infra_running || { warn "Infra not detected - starting it first"; infra_up || return 1; }
    fi

    local s
    for s in "${to_start[@]}"; do
        start_service "$s" || return 1
        wait_healthy "$s" "$(port_of "$s")" || return 1
    done
    ok "Start complete"
}

cmd_stop() {
    local to_stop=()
    if [[ $# -eq 0 ]] || [[ "${1:-}" == "all" ]]; then
        local i
        for (( i=${#SERVICE_ORDER[@]}-1; i>=0; i-- )); do to_stop+=("${SERVICE_ORDER[$i]}"); done
    else
        local t
        for t in "$@"; do
            if [[ "$t" == "infra" ]]; then
                infra_down
            elif is_valid_service "$t"; then
                to_stop+=("$t")
            else
                err "Unknown service '$t' - valid: ${SERVICE_ORDER[*]} | infra | all"
                return 1
            fi
        done
    fi
    local s
    for s in "${to_stop[@]}"; do stop_service "$s"; done
    if [[ $# -eq 0 ]] || [[ "${1:-}" == "all" ]]; then
        log "Tip: run 'scripts/swiftbasket.sh stop infra' to also stop the containers"
    fi
    ok "Stop complete"
}

cmd_restart() {
    if [[ $# -ne 1 ]] || ! is_valid_service "$1"; then
        err "Usage: scripts/swiftbasket.sh restart <service>"
        return 1
    fi
    # shellcheck disable=SC1090
    [[ -f "$ENV_FILE" ]] && set -a && source "$ENV_FILE" && set +a
    stop_service "$1"
    start_service "$1" || return 1
    wait_healthy "$1" "$(port_of "$1")"
}

cmd_status() {
    printf "\n%-14s %-7s %-7s %-9s %s\n" "SERVICE" "PORT" "PID" "STATE" "HEALTH"
    printf -- "------------------------------------------------------\n"
    local s port pid state health
    for s in "${SERVICE_ORDER[@]}"; do
        port="$(port_of "$s")"
        pid="-"; state="${RED}stopped${NC}"
        if pid_of "$s"; then
            pid="$(cat "$PID_DIR/$s.pid")"
            state="${GREEN}running${NC}"
        fi
        if health_ok "$s" "$port"; then health="${GREEN}healthy${NC}"; else health="${RED}down${NC}"; fi
        printf "%-14s %-7s %-7s %-18s %b\n" "$s" "$port" "$pid" "$state" "$health"
    done
    printf -- "------------------------------------------------------\n"
    if infra_running; then
        printf "%-14s %-7s %-7s %-18s %b\n" "infra" "-" "-" "${GREEN}running${NC}" "${GREEN}healthy${NC}"
    else
        printf "%-14s %-7s %-7s %-18s %b\n" "infra" "-" "-" "${RED}stopped${NC}" "${RED}down${NC}"
    fi
    # no bare /health route on Kong - probe a routed service health endpoint
    if curl -sf -o /dev/null --max-time 3 "http://localhost:$GATEWAY_PORT/api/v1/auth/health"; then
        printf "%-14s %-7s %-7s %-9s %b\n" "gateway" "$GATEWAY_PORT" "-" "-" "${GREEN}healthy${NC}"
    else
        printf "%-14s %-7s %-7s %-9s %b\n" "gateway" "$GATEWAY_PORT" "-" "-" "${RED}down${NC}"
    fi
    echo ""
}

cmd_logs() {
    if [[ $# -ne 1 ]] || ! is_valid_service "$1"; then
        err "Usage: scripts/swiftbasket.sh logs <service> - valid: ${SERVICE_ORDER[*]}"
        return 1
    fi
    local logfile="$LOG_DIR/$1.log"
    if [[ ! -f "$logfile" ]]; then
        err "No log yet for $1 at $logfile"
        return 1
    fi
    log "Tailing $logfile (Ctrl-C to exit)"
    tail -f "$logfile"
}

usage() {
    cat <<EOF
SwiftBasket service manager

Usage: scripts/swiftbasket.sh <command> [args]

Commands:
  start   [service|infra|all]   Start service(s) in dependency order; 'all' (default)
                                auto-starts infra containers if they are down.
                                Waits for each /health before starting the next.
  stop    [service|infra|all]   Stop service(s); 'all' (default) stops in reverse
                                dependency order. Infra kept running unless 'infra'.
  restart <service>             Restart a single service.
  status                        Health-check table: ports, pids, state, health.
  logs    <service>             Tail a service's log file (Ctrl-C to exit).

Services (start order): ${SERVICE_ORDER[*]}
Ports: user=8081 product=8082 cart=8083 order=8084 payment=8085 notification=8086
Gateway (Kong): $GATEWAY_PORT
Logs: logs/<service>.log   Pids: logs/<service>.pid

Env: an optional scripts/.env (copied from scripts/.env.example) is sourced before start/restart
     (MAILTRAP_USERNAME, MAILTRAP_PASSWORD, STRIPE_API_KEY, STRIPE_WEBHOOK_SECRET, JWT_SECRET...).
EOF
}

# ----------------------------------------------------------------- main -----

case "${1:-}" in
    start)   shift; cmd_start "$@" ;;
    stop)    shift; cmd_stop "$@" ;;
    restart) shift; cmd_restart "$@" ;;
    status)  cmd_status ;;
    logs)    shift; cmd_logs "$@" ;;
    *)       usage; exit 1 ;;
esac
