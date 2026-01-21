#!/bin/bash
# cleanup.sh - Remove old Jibri recordings
# Add to crontab: 0 3 * * * /opt/jibri/cleanup.sh
#
# This script removes recordings older than 90 days to save disk space

RECORDINGS_DIR="/var/jibri/recordings"
MAX_AGE_DAYS=90
LOG_FILE="/var/log/jibri/cleanup.log"

log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" >> "$LOG_FILE"
}

log "=== Cleanup started ==="

# Count files before cleanup
BEFORE_COUNT=$(find "$RECORDINGS_DIR" -name "*.mp4" -type f | wc -l)
log "Files before cleanup: $BEFORE_COUNT"

# Delete MP4 files older than MAX_AGE_DAYS
DELETED=$(find "$RECORDINGS_DIR" -name "*.mp4" -type f -mtime +$MAX_AGE_DAYS -delete -print | wc -l)
log "Deleted $DELETED MP4 files older than $MAX_AGE_DAYS days"

# Remove empty directories
EMPTY_DIRS=$(find "$RECORDINGS_DIR" -type d -empty -delete -print | wc -l)
log "Removed $EMPTY_DIRS empty directories"

# Count files after cleanup
AFTER_COUNT=$(find "$RECORDINGS_DIR" -name "*.mp4" -type f | wc -l)
log "Files after cleanup: $AFTER_COUNT"

# Calculate disk space used
DISK_USAGE=$(du -sh "$RECORDINGS_DIR" 2>/dev/null | cut -f1)
log "Current disk usage: $DISK_USAGE"

log "=== Cleanup completed ==="
