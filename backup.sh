#!/bin/bash
# =============================================================================
# ChessConnect - Script de backup PostgreSQL (Production)
# Usage: ./backup.sh [daily|weekly|monthly|manual]
# =============================================================================

set -e

# Configuration
BACKUP_DIR="./backups"
LOG_DIR="./backups/logs"
CONTAINER_NAME="mychess-db"
DATE=$(date +%Y%m%d_%H%M%S)
DATE_HUMAN=$(date '+%Y-%m-%d %H:%M:%S')

# Retention policies (in days)
RETENTION_DAILY=${BACKUP_RETENTION_DAILY:-7}
RETENTION_WEEKLY=${BACKUP_RETENTION_WEEKLY:-30}
RETENTION_MONTHLY=${BACKUP_RETENTION_MONTHLY:-365}

# Load environment variables
if [ -f .env ]; then
    export $(grep -v '^#' .env | xargs)
fi

DB_NAME="${POSTGRES_DB:-chessconnect}"
DB_USER="${POSTGRES_USER:-chess}"

# Backup type (daily, weekly, monthly, manual)
BACKUP_TYPE="${1:-manual}"

# Create directories
mkdir -p "$BACKUP_DIR"
mkdir -p "$LOG_DIR"

# Log file
LOG_FILE="$LOG_DIR/backup_${DATE}.log"

# Logging function
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG_FILE"
}

# Error handler
error_exit() {
    log "ERROR: $1"

    # Send alert (if configured)
    if [ -n "$ALERT_EMAIL" ] && command -v mail &> /dev/null; then
        echo "Backup failed: $1" | mail -s "[ChessConnect] Backup FAILED - $DATE_HUMAN" "$ALERT_EMAIL"
    fi

    exit 1
}

# Backup filename
BACKUP_FILE="$BACKUP_DIR/${DB_NAME}_${BACKUP_TYPE}_${DATE}.sql.gz"
CHECKSUM_FILE="${BACKUP_FILE}.sha256"

log "========================================"
log "ChessConnect Backup - $BACKUP_TYPE"
log "========================================"
log "Database: $DB_NAME"
log "Container: $CONTAINER_NAME"

# Check container is running
if ! docker ps --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
    error_exit "Container $CONTAINER_NAME is not running"
fi

# Check disk space (at least 1GB free)
FREE_SPACE=$(df -BG "$BACKUP_DIR" | awk 'NR==2 {print $4}' | tr -d 'G')
if [ "$FREE_SPACE" -lt 1 ]; then
    error_exit "Insufficient disk space: ${FREE_SPACE}GB available"
fi
log "Disk space available: ${FREE_SPACE}GB"

# Create backup
log "Creating backup..."
START_TIME=$(date +%s)

docker exec "$CONTAINER_NAME" pg_dump -U "$DB_USER" -d "$DB_NAME" \
    --no-owner --no-acl --format=plain \
    --verbose 2>>"$LOG_FILE" | gzip > "$BACKUP_FILE"

END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))

# Verify backup
if [ ! -f "$BACKUP_FILE" ] || [ ! -s "$BACKUP_FILE" ]; then
    error_exit "Backup file is empty or missing"
fi

# Create checksum
sha256sum "$BACKUP_FILE" > "$CHECKSUM_FILE"
log "Checksum created: $(cat $CHECKSUM_FILE | cut -d' ' -f1)"

# Get backup size
SIZE=$(du -h "$BACKUP_FILE" | cut -f1)
SIZE_BYTES=$(stat -c %s "$BACKUP_FILE")

log "Backup created successfully!"
log "  File: $BACKUP_FILE"
log "  Size: $SIZE ($SIZE_BYTES bytes)"
log "  Duration: ${DURATION}s"

# Verify backup integrity (test gunzip)
log "Verifying backup integrity..."
if ! gunzip -t "$BACKUP_FILE" 2>/dev/null; then
    error_exit "Backup integrity check failed"
fi
log "Integrity check: OK"

# Count records in backup
TABLES_COUNT=$(zcat "$BACKUP_FILE" | grep -c "^CREATE TABLE" || echo "0")
log "Tables in backup: $TABLES_COUNT"

# Cleanup old backups based on type
log ""
log "Cleaning up old backups..."

case $BACKUP_TYPE in
    daily)
        RETENTION=$RETENTION_DAILY
        ;;
    weekly)
        RETENTION=$RETENTION_WEEKLY
        ;;
    monthly)
        RETENTION=$RETENTION_MONTHLY
        ;;
    *)
        RETENTION=$RETENTION_DAILY
        ;;
esac

# Remove old backups of the same type
DELETED_COUNT=$(find "$BACKUP_DIR" -name "*_${BACKUP_TYPE}_*.sql.gz" -mtime +$RETENTION -delete -print | wc -l)
log "Deleted $DELETED_COUNT old $BACKUP_TYPE backups (retention: $RETENTION days)"

# Remove old checksums
find "$BACKUP_DIR" -name "*.sha256" -mtime +$RETENTION -delete 2>/dev/null || true

# Remove old logs (keep 30 days)
find "$LOG_DIR" -name "*.log" -mtime +30 -delete 2>/dev/null || true

# List current backups
log ""
log "Current backups:"
ls -lhS "$BACKUP_DIR"/*.sql.gz 2>/dev/null | head -20 | while read line; do
    log "  $line"
done

# Summary
TOTAL_BACKUPS=$(ls -1 "$BACKUP_DIR"/*.sql.gz 2>/dev/null | wc -l)
TOTAL_SIZE=$(du -sh "$BACKUP_DIR" 2>/dev/null | cut -f1)

log ""
log "========================================"
log "Backup Summary"
log "========================================"
log "  Status: SUCCESS"
log "  Type: $BACKUP_TYPE"
log "  File: $BACKUP_FILE"
log "  Size: $SIZE"
log "  Duration: ${DURATION}s"
log "  Total backups: $TOTAL_BACKUPS"
log "  Total size: $TOTAL_SIZE"
log "========================================"

# Optional: Copy to remote storage
if [ -n "$BACKUP_REMOTE_PATH" ]; then
    log "Copying to remote storage: $BACKUP_REMOTE_PATH"
    if command -v rsync &> /dev/null; then
        rsync -avz "$BACKUP_FILE" "$CHECKSUM_FILE" "$BACKUP_REMOTE_PATH/" 2>>"$LOG_FILE"
        log "Remote copy: OK"
    else
        log "Warning: rsync not available, skipping remote copy"
    fi
fi

exit 0
