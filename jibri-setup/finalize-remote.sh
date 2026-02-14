#!/bin/bash
# finalize-remote.sh - Called by Jibri on REMOTE server (server 2) when a recording finishes
# 1. SCPs the recording to the main server (meet.mychess.fr)
# 2. Notifies the backend API via HMAC-SHA256 signed webhook

set -e

# Load environment variables (Jibri subprocess doesn't inherit systemd env)
[ -f /etc/jibri/env ] && . /etc/jibri/env

RECORDING_DIR="$1"
MAIN_SERVER="${MAIN_SERVER_IP:-5.189.173.131}"
MAIN_SERVER_RECORDINGS="/var/jibri/recordings"
BACKEND_URL="${BACKEND_WEBHOOK_URL:-https://mychess.fr/api/recordings/webhook}"
WEBHOOK_SECRET="${JIBRI_WEBHOOK_SECRET:-}"
LOG_FILE="/var/log/jibri/finalize.log"

log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] [remote] $1" >> "$LOG_FILE"
}

log "=== Remote finalize script started ==="
log "Recording directory: $RECORDING_DIR"

# Find the MP4 file in the recording directory
MP4_FILE=$(find "$RECORDING_DIR" -name "*.mp4" -type f | head -1)

if [ -z "$MP4_FILE" ]; then
    log "ERROR: No MP4 file found in $RECORDING_DIR"
    exit 1
fi

FILENAME=$(basename "$MP4_FILE")
ROOM_NAME=$(basename "$RECORDING_DIR")
REMOTE_DIR="${MAIN_SERVER_RECORDINGS}/${ROOM_NAME}"

log "MP4 file: $MP4_FILE"
log "Filename: $FILENAME"
log "Room name: $ROOM_NAME"

# Step 1: Create remote directory and SCP the recording to main server
log "Creating remote directory on main server: $REMOTE_DIR"
ssh -o StrictHostKeyChecking=no -o ConnectTimeout=10 "root@${MAIN_SERVER}" "mkdir -p '${REMOTE_DIR}'"

log "Transferring recording to main server via SCP..."
SCP_START=$(date +%s)
scp -o StrictHostKeyChecking=no -o ConnectTimeout=10 \
    "$MP4_FILE" "root@${MAIN_SERVER}:${REMOTE_DIR}/${FILENAME}"
SCP_END=$(date +%s)
SCP_DURATION=$((SCP_END - SCP_START))
log "SCP transfer completed in ${SCP_DURATION}s"

# The path on the main server where the backend can find the file
MAIN_SERVER_PATH="${REMOTE_DIR}/${FILENAME}"

# Step 2: Build JSON payload with the MAIN SERVER path
PAYLOAD="{\"filename\": \"$FILENAME\", \"path\": \"$MAIN_SERVER_PATH\", \"room\": \"$ROOM_NAME\"}"

# Compute HMAC-SHA256 signature if secret is configured
if [ -n "$WEBHOOK_SECRET" ]; then
    SIGNATURE=$(echo -n "$PAYLOAD" | openssl dgst -sha256 -hmac "$WEBHOOK_SECRET" | awk '{print $2}')
    log "HMAC signature computed"
else
    log "WARNING: JIBRI_WEBHOOK_SECRET not set - sending without signature"
fi

# Step 3: Call the backend webhook
log "Calling backend webhook at $BACKEND_URL"

if [ -n "$WEBHOOK_SECRET" ]; then
    RESPONSE=$(curl -s -w "\n%{http_code}" -X POST \
        -H "Content-Type: application/json" \
        -H "X-Jibri-Signature: sha256=$SIGNATURE" \
        -d "$PAYLOAD" \
        "$BACKEND_URL" \
        --max-time 30 \
        --retry 3 \
        --retry-delay 5)
else
    RESPONSE=$(curl -s -w "\n%{http_code}" -X POST \
        -H "Content-Type: application/json" \
        -d "$PAYLOAD" \
        "$BACKEND_URL" \
        --max-time 30 \
        --retry 3 \
        --retry-delay 5)
fi

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')

log "HTTP response code: $HTTP_CODE"
log "Response body: $BODY"

if [ "$HTTP_CODE" -ge 200 ] && [ "$HTTP_CODE" -lt 300 ]; then
    log "SUCCESS: Webhook called successfully"
    # Clean up local recording to save disk space on server 2
    log "Cleaning up local recording: $MP4_FILE"
    rm -f "$MP4_FILE"
    rmdir "$RECORDING_DIR" 2>/dev/null || true
    log "Local cleanup done"
else
    log "WARNING: Webhook returned non-success code: $HTTP_CODE"
    log "Keeping local recording as backup: $MP4_FILE"
fi

log "=== Remote finalize script completed ==="
