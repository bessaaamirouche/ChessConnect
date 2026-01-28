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

# Rendre le script exécutable
chmod +x "$BACKUP_SCRIPT"

# Créer le fichier cron
CRON_FILE="/etc/cron.d/mychess-backup"

cat > "$CRON_FILE" << EOF
# ChessConnect - Backup automatique PostgreSQL
SHELL=/bin/bash
PATH=/usr/local/sbin:/usr/local/bin:/sbin:/bin:/usr/sbin:/usr/bin

# Backup quotidien à 3h du matin (retention: 7 jours)
0 3 * * * root cd $SCRIPT_DIR && ./backup.sh daily >> /var/log/mychess-backup.log 2>&1

# Backup hebdomadaire le dimanche à 4h du matin (retention: 30 jours)
0 4 * * 0 root cd $SCRIPT_DIR && ./backup.sh weekly >> /var/log/mychess-backup.log 2>&1

# Backup mensuel le 1er du mois à 5h du matin (retention: 365 jours)
0 5 1 * * root cd $SCRIPT_DIR && ./backup.sh monthly >> /var/log/mychess-backup.log 2>&1

# VACUUM ANALYZE hebdomadaire le dimanche à 2h du matin
0 2 * * 0 root docker exec mychess-db psql -U chess -d mychess -c "VACUUM ANALYZE;" >> /var/log/mychess-vacuum.log 2>&1

# Nettoyage des logs de plus de 30 jours
0 6 * * 0 root find /var/log -name "mychess-*.log" -mtime +30 -delete 2>/dev/null
EOF

chmod 644 "$CRON_FILE"

# Créer le fichier de rotation des logs
LOGROTATE_FILE="/etc/logrotate.d/mychess"
cat > "$LOGROTATE_FILE" << EOF
/var/log/mychess-*.log {
    weekly
    rotate 4
    compress
    delaycompress
    missingok
    notifempty
    create 644 root root
}
EOF

chmod 644 "$LOGROTATE_FILE"

echo "========================================"
echo "Cron de backup installé avec succès!"
echo "========================================"
echo ""
echo "Fichiers créés:"
echo "  - Cron: $CRON_FILE"
echo "  - Logrotate: $LOGROTATE_FILE"
echo ""
echo "Backups programmés:"
echo "  - Quotidien:    tous les jours à 3h00 (retention: 7 jours)"
echo "  - Hebdomadaire: dimanche à 4h00 (retention: 30 jours)"
echo "  - Mensuel:      1er du mois à 5h00 (retention: 365 jours)"
echo ""
echo "Maintenance DB:"
echo "  - VACUUM ANALYZE: dimanche à 2h00"
echo ""
echo "Logs:"
echo "  - Backup: /var/log/mychess-backup.log"
echo "  - Vacuum: /var/log/mychess-vacuum.log"
echo ""
echo "Commandes utiles:"
echo "  - Voir les crons: cat /etc/cron.d/mychess-backup"
echo "  - Backup manuel:  ./backup.sh manual"
echo "  - Voir les logs:  tail -f /var/log/mychess-backup.log"
