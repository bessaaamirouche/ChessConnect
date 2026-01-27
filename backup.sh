#!/bin/bash
# =============================================================================
# ChessConnect - Script de backup PostgreSQL
# Usage: ./backup.sh [daily|weekly|manual]
# =============================================================================

set -e

# Configuration
BACKUP_DIR="./backups"
CONTAINER_NAME="chessconnect-db"
DATE=$(date +%Y%m%d_%H%M%S)
RETENTION_DAYS=${BACKUP_RETENTION_DAYS:-7}

# Charger les variables d'environnement
if [ -f .env ]; then
    export $(grep -v '^#' .env | xargs)
fi

DB_NAME="${POSTGRES_DB:-chessconnect}"
DB_USER="${POSTGRES_USER:-chess}"

# Type de backup (daily, weekly, manual)
BACKUP_TYPE="${1:-manual}"

# Créer le dossier de backup si nécessaire
mkdir -p "$BACKUP_DIR"

# Nom du fichier de backup
BACKUP_FILE="$BACKUP_DIR/${DB_NAME}_${BACKUP_TYPE}_${DATE}.sql.gz"

echo "========================================"
echo "ChessConnect Backup - $(date)"
echo "========================================"
echo "Type: $BACKUP_TYPE"
echo "Database: $DB_NAME"
echo "Container: $CONTAINER_NAME"
echo ""

# Vérifier que le conteneur est en cours d'exécution
if ! docker ps --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
    echo "ERREUR: Le conteneur $CONTAINER_NAME n'est pas en cours d'exécution"
    exit 1
fi

# Effectuer le backup
echo "Création du backup..."
docker exec "$CONTAINER_NAME" pg_dump -U "$DB_USER" -d "$DB_NAME" --no-owner --no-acl | gzip > "$BACKUP_FILE"

# Vérifier que le backup a réussi
if [ -f "$BACKUP_FILE" ] && [ -s "$BACKUP_FILE" ]; then
    SIZE=$(du -h "$BACKUP_FILE" | cut -f1)
    echo "Backup créé avec succès: $BACKUP_FILE ($SIZE)"
else
    echo "ERREUR: Le backup a échoué"
    rm -f "$BACKUP_FILE"
    exit 1
fi

# Nettoyer les anciens backups (garder les N derniers jours)
echo ""
echo "Nettoyage des backups de plus de $RETENTION_DAYS jours..."
find "$BACKUP_DIR" -name "*.sql.gz" -mtime +$RETENTION_DAYS -delete 2>/dev/null || true

# Lister les backups existants
echo ""
echo "Backups disponibles:"
ls -lh "$BACKUP_DIR"/*.sql.gz 2>/dev/null | tail -10 || echo "Aucun backup trouvé"

echo ""
echo "========================================"
echo "Backup terminé avec succès"
echo "========================================"
