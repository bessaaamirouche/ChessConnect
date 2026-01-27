#!/bin/bash
# =============================================================================
# ChessConnect - Script de restauration PostgreSQL
# Usage: ./restore.sh <backup_file.sql.gz>
# =============================================================================

set -e

# Configuration
CONTAINER_NAME="chessconnect-db"

# Charger les variables d'environnement
if [ -f .env ]; then
    export $(grep -v '^#' .env | xargs)
fi

DB_NAME="${POSTGRES_DB:-chessconnect}"
DB_USER="${POSTGRES_USER:-chess}"

# Vérifier les arguments
if [ -z "$1" ]; then
    echo "Usage: $0 <backup_file.sql.gz>"
    echo ""
    echo "Backups disponibles:"
    ls -lh ./backups/*.sql.gz 2>/dev/null || echo "Aucun backup trouvé dans ./backups/"
    exit 1
fi

BACKUP_FILE="$1"

if [ ! -f "$BACKUP_FILE" ]; then
    echo "ERREUR: Le fichier $BACKUP_FILE n'existe pas"
    exit 1
fi

echo "========================================"
echo "ChessConnect Restore - $(date)"
echo "========================================"
echo "Fichier: $BACKUP_FILE"
echo "Database: $DB_NAME"
echo "Container: $CONTAINER_NAME"
echo ""

# Avertissement
echo "ATTENTION: Cette opération va SUPPRIMER toutes les données actuelles!"
read -p "Êtes-vous sûr de vouloir continuer? (yes/no): " CONFIRM

if [ "$CONFIRM" != "yes" ]; then
    echo "Restauration annulée"
    exit 0
fi

# Vérifier que le conteneur est en cours d'exécution
if ! docker ps --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
    echo "ERREUR: Le conteneur $CONTAINER_NAME n'est pas en cours d'exécution"
    exit 1
fi

# Arrêter le backend pour éviter les connexions
echo "Arrêt du backend..."
docker stop chessconnect-backend 2>/dev/null || true

# Recréer la base de données
echo "Recréation de la base de données..."
docker exec "$CONTAINER_NAME" psql -U "$DB_USER" -d postgres -c "DROP DATABASE IF EXISTS $DB_NAME;"
docker exec "$CONTAINER_NAME" psql -U "$DB_USER" -d postgres -c "CREATE DATABASE $DB_NAME;"

# Restaurer le backup
echo "Restauration du backup..."
gunzip -c "$BACKUP_FILE" | docker exec -i "$CONTAINER_NAME" psql -U "$DB_USER" -d "$DB_NAME"

# Redémarrer le backend
echo "Redémarrage du backend..."
docker start chessconnect-backend

echo ""
echo "========================================"
echo "Restauration terminée avec succès"
echo "========================================"
