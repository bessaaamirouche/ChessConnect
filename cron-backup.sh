#!/bin/bash
# =============================================================================
# ChessConnect - Installation du cron de backup automatique
# Usage: sudo ./cron-backup.sh
# =============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKUP_SCRIPT="$SCRIPT_DIR/backup.sh"

# Vérifier que le script de backup existe
if [ ! -f "$BACKUP_SCRIPT" ]; then
    echo "ERREUR: $BACKUP_SCRIPT n'existe pas"
    exit 1
fi

# Créer le fichier cron
CRON_FILE="/etc/cron.d/chessconnect-backup"

cat > "$CRON_FILE" << EOF
# ChessConnect - Backup automatique PostgreSQL
# Backup quotidien à 3h du matin
0 3 * * * root cd $SCRIPT_DIR && ./backup.sh daily >> /var/log/chessconnect-backup.log 2>&1

# Backup hebdomadaire le dimanche à 4h du matin
0 4 * * 0 root cd $SCRIPT_DIR && ./backup.sh weekly >> /var/log/chessconnect-backup.log 2>&1
EOF

chmod 644 "$CRON_FILE"

echo "Cron de backup installé: $CRON_FILE"
echo ""
echo "Backups programmés:"
echo "  - Quotidien: tous les jours à 3h00"
echo "  - Hebdomadaire: dimanche à 4h00"
echo ""
echo "Logs: /var/log/chessconnect-backup.log"
