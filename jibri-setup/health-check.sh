#!/bin/bash
# =============================================================================
# Jibri Health Check Script
# =============================================================================
# Checks all running Jibri instances every 5 minutes (via cron).
# Detects multiple types of failures:
#   1. HTTP health endpoint down or unhealthy
#   2. XMPP connection silently dead (no XMPP traffic in logs)
#   3. TCP connection to Prosody missing (port 5222)
# Restarts only affected instances. Never interrupts active recordings.
# Safety: max restarts per cycle + post-restart verification.
#
# Cron: */5 * * * * /opt/jibri/health-check.sh
# =============================================================================

set -euo pipefail

# ---------------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------------
XMPP_TIMEOUT_MIN=10        # Minutes without XMPP activity = consider dead
MIN_UPTIME_MIN=15           # Don't restart instances younger than this
HTTP_TIMEOUT=5              # Seconds to wait for HTTP health response
MAX_LOG_SIZE=2097152        # 2 MB - rotate log if larger
MAX_RESTARTS_PER_CYCLE=4    # Safety: max restarts per 5-min cycle (prevent storms)
PROSODY_PORT=5222           # Prosody XMPP port
POST_RESTART_WAIT=10        # Seconds to wait after restart before verification
LOG_FILE="/var/log/jibri/health-check.log"
LOCK_FILE="/tmp/jibri-health-check.lock"

# ---------------------------------------------------------------------------
# Prevent concurrent runs (flock)
# ---------------------------------------------------------------------------
exec 200>"$LOCK_FILE"
if ! flock -n 200; then
    exit 0
fi

# ---------------------------------------------------------------------------
# Logging
# ---------------------------------------------------------------------------
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" >> "$LOG_FILE"
}

# Rotate log if too large
if [ -f "$LOG_FILE" ]; then
    log_size=$(stat -c%s "$LOG_FILE" 2>/dev/null || echo 0)
    if [ "$log_size" -gt "$MAX_LOG_SIZE" ]; then
        mv "$LOG_FILE" "${LOG_FILE}.old"
        log "Log rotated (previous size: ${log_size} bytes)"
    fi
fi

# ---------------------------------------------------------------------------
# Discover running Jibri containers
# ---------------------------------------------------------------------------
INSTANCES=$(docker ps --filter "name=^jibri-[0-9]" --filter "status=running" --format "{{.Names}}" 2>/dev/null | sort -V)

if [ -z "$INSTANCES" ]; then
    log "WARNING: No running Jibri containers found"
    exit 0
fi

TOTAL=$(echo "$INSTANCES" | wc -l)
RESTARTED=0
BUSY=0
OK=0
SKIPPED=0
FAILED_RESTART=0
RESTARTED_LIST=""

log "--- Health check: ${TOTAL} instance(s) ---"

# ---------------------------------------------------------------------------
# Helper: safe restart with cycle limit and post-restart verification
# ---------------------------------------------------------------------------
safe_restart() {
    local inst="$1"
    local reason="$2"

    # Check restart limit
    if [ "$RESTARTED" -ge "$MAX_RESTARTS_PER_CYCLE" ]; then
        log "SKIP $inst: restart limit reached ($MAX_RESTARTS_PER_CYCLE/cycle) - $reason"
        SKIPPED=$((SKIPPED + 1))
        return 1
    fi

    log "RESTART $inst: $reason"
    docker restart "$inst" >/dev/null 2>&1
    RESTARTED=$((RESTARTED + 1))
    RESTARTED_LIST="${RESTARTED_LIST} ${inst}"
    return 0
}

# ---------------------------------------------------------------------------
# Check each instance
# ---------------------------------------------------------------------------
for instance in $INSTANCES; do
    # Derive HTTP API port: jibri-N → port 2221 + N
    num=$(echo "$instance" | grep -oE '[0-9]+$')
    port=$((2221 + num))

    # ------ Check 1: HTTP health endpoint ------
    health=$(curl -s --connect-timeout "$HTTP_TIMEOUT" --max-time 10 \
        "http://localhost:${port}/jibri/api/v1.0/health" 2>/dev/null || true)

    if [ -z "$health" ]; then
        safe_restart "$instance" "HTTP health not responding (port $port)" || true
        continue
    fi

    # Parse JSON response
    busy_status=$(echo "$health" | python3 -c "
import sys, json
try:
    d = json.loads(sys.stdin.read())
    print(d.get('status',{}).get('busyStatus','UNKNOWN'))
except: print('UNKNOWN')
" 2>/dev/null)

    health_status=$(echo "$health" | python3 -c "
import sys, json
try:
    d = json.loads(sys.stdin.read())
    print(d.get('status',{}).get('health',{}).get('healthStatus','UNKNOWN'))
except: print('UNKNOWN')
" 2>/dev/null)

    # ------ Check 2: Health status ------
    if [ "$health_status" != "HEALTHY" ]; then
        safe_restart "$instance" "unhealthy ($health_status)" || true
        continue
    fi

    # ------ Check 3: Skip if recording in progress ------
    if [ "$busy_status" = "BUSY" ]; then
        log "OK $instance: BUSY (recording)"
        BUSY=$((BUSY + 1))
        continue
    fi

    # ------ Check 4: XMPP connection vitality ------
    # Calculate container uptime in minutes
    started_at=$(docker inspect --format '{{.State.StartedAt}}' "$instance" 2>/dev/null || true)
    uptime_min=0
    if [ -n "$started_at" ]; then
        started_epoch=$(date -d "$started_at" +%s 2>/dev/null || echo 0)
        now_epoch=$(date +%s)
        uptime_min=$(( (now_epoch - started_epoch) / 60 ))
    fi

    # Don't check XMPP on recently restarted instances
    if [ "$uptime_min" -lt "$MIN_UPTIME_MIN" ]; then
        log "OK $instance: IDLE, uptime ${uptime_min}m (< ${MIN_UPTIME_MIN}m, skip XMPP)"
        OK=$((OK + 1))
        continue
    fi

    # ------ Check 5: TCP connection to Prosody (port 5222) ------
    # With network_mode:host, all containers share the host network stack.
    # Find the Java PID inside this container and check if it has a TCP
    # connection to Prosody.
    java_pid=$(docker top "$instance" -eo pid,comm 2>/dev/null | grep java | awk '{print $1}' | head -1)
    tcp_conn=0
    if [ -n "$java_pid" ]; then
        tcp_conn=$(ss -tnp state established "( dport = :${PROSODY_PORT} )" 2>/dev/null \
            | grep -c "pid=${java_pid}," || true)
        tcp_conn=$(echo "$tcp_conn" | tail -1 | tr -dc '0-9')
        tcp_conn=${tcp_conn:-0}
    fi

    if [ "$tcp_conn" -eq 0 ] 2>/dev/null; then
        safe_restart "$instance" "no TCP connection to Prosody:${PROSODY_PORT} (uptime: ${uptime_min}m)" || true
        continue
    fi

    # ------ Check 6: XMPP log activity ------
    # Count XMPP-related log entries in the last XMPP_TIMEOUT_MIN minutes
    xmpp_count=$(docker logs --since "${XMPP_TIMEOUT_MIN}m" "$instance" 2>&1 \
        | grep -cE "MucClient|presence|JibriIq|PacketExtension|xmpp\." || true)
    # Ensure clean integer (grep -c can output multi-line in edge cases)
    xmpp_count=$(echo "$xmpp_count" | tail -1 | tr -dc '0-9')
    xmpp_count=${xmpp_count:-0}

    if [ "$xmpp_count" -eq 0 ] 2>/dev/null; then
        safe_restart "$instance" "IDLE, no XMPP for ${XMPP_TIMEOUT_MIN}m (uptime: ${uptime_min}m, TCP: ${tcp_conn})" || true
        continue
    fi

    log "OK $instance: IDLE, XMPP ok (${xmpp_count} in ${XMPP_TIMEOUT_MIN}m, TCP: ${tcp_conn})"
    OK=$((OK + 1))
done

# ---------------------------------------------------------------------------
# Post-restart verification
# ---------------------------------------------------------------------------
if [ -n "$RESTARTED_LIST" ]; then
    log "--- Post-restart verification (waiting ${POST_RESTART_WAIT}s) ---"
    sleep "$POST_RESTART_WAIT"

    for inst in $RESTARTED_LIST; do
        num=$(echo "$inst" | grep -oE '[0-9]+$')
        port=$((2221 + num))

        # Check if container is running
        running=$(docker inspect --format '{{.State.Running}}' "$inst" 2>/dev/null || echo "false")
        if [ "$running" != "true" ]; then
            log "ALERT $inst: NOT RUNNING after restart!"
            FAILED_RESTART=$((FAILED_RESTART + 1))
            continue
        fi

        # Check HTTP health (with shorter timeout since it just started)
        post_health=$(curl -s --connect-timeout 3 --max-time 5 \
            "http://localhost:${port}/jibri/api/v1.0/health" 2>/dev/null || true)
        if [ -n "$post_health" ]; then
            log "VERIFIED $inst: back online (HTTP responding)"
        else
            log "WARN $inst: HTTP not yet responding (may need more time)"
        fi
    done
fi

# ---------------------------------------------------------------------------
# Summary
# ---------------------------------------------------------------------------
summary="OK=$OK BUSY=$BUSY RESTARTED=$RESTARTED"
[ "$SKIPPED" -gt 0 ] && summary="$summary SKIPPED=$SKIPPED"
[ "$FAILED_RESTART" -gt 0 ] && summary="$summary FAILED=$FAILED_RESTART"
log "--- Result: $summary ---"
