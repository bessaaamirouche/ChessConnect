#!/bin/bash
# finalize.sh - Called by Jibri when a recording finishes
# This script notifies the backend API about the completed recording

set -e

RECORDING_DIR="$1"
BACKEND_URL="${BACKEND_WEBHOOK_URL:-https://api.mychess.fr/api/recordings/webhook}"
LOG_FILE="/var/log/jibri/finalize.log"

log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" >> "$LOG_FILE"
}

log "=== Finalize script started ==="
log "Recording directory: $RECORDING_DIR"

# Find the MP4 file in the recording directory
MP4_FILE=$(find "$RECORDING_DIR" -name "*.mp4" -type f | head -1)

if [ -z "$MP4_FILE" ]; then
    log "ERROR: No MP4 file found in $RECORDING_DIR"
    exit 1
fi

FILENAME=$(basename "$MP4_FILE")
ROOM_NAME=$(basename "$RECORDING_DIR")

log "MP4 file: $MP4_FILE"
log "Filename: $FILENAME"
log "Room name: $ROOM_NAME"

# Call the backend webhook
log "Calling backend webhook at $BACKEND_URL"

RESPONSE=$(curl -s -w "\n%{http_code}" -X POST \
    -H "Content-Type: application/json" \
    -d "{\"filename\": \"$FILENAME\", \"path\": \"$MP4_FILE\", \"room\": \"$ROOM_NAME\"}" \
    "$BACKEND_URL" \
    --max-time 30 \
    --retry 3 \
    --retry-delay 5)

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')

log "HTTP response code: $HTTP_CODE"
log "Response body: $BODY"

if [ "$HTTP_CODE" -ge 200 ] && [ "$HTTP_CODE" -lt 300 ]; then
    log "SUCCESS: Webhook called successfully"
else
    log "WARNING: Webhook returned non-success code: $HTTP_CODE"
fi

log "=== Finalize script completed ==="
