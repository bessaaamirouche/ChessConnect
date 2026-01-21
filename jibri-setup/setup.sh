#!/bin/bash
# Jibri Setup Script for meet.mychess.fr
# Run this script on the Jitsi server as root
#
# Usage: sudo ./setup.sh

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

log_info() { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

# Check if running as root
if [ "$EUID" -ne 0 ]; then
    log_error "Please run as root (sudo ./setup.sh)"
    exit 1
fi

# Configuration
JIBRI_PASSWORD=$(openssl rand -hex 16)
RECORDER_PASSWORD=$(openssl rand -hex 16)
JIBRI_DIR="/opt/jibri"
RECORDINGS_DIR="/var/jibri/recordings"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

log_info "=== Jibri Setup for meet.mychess.fr ==="
echo ""

# Step 1: Create directories
log_info "Step 1: Creating directories..."
mkdir -p "$JIBRI_DIR"
mkdir -p "$RECORDINGS_DIR"
mkdir -p /var/log/jibri
chmod 777 "$RECORDINGS_DIR"
chmod 755 /var/log/jibri
log_info "Directories created: $JIBRI_DIR, $RECORDINGS_DIR"

# Step 2: Copy files
log_info "Step 2: Copying configuration files..."
cp "$SCRIPT_DIR/docker-compose.yml" "$JIBRI_DIR/"
cp "$SCRIPT_DIR/finalize.sh" "$JIBRI_DIR/"
cp "$SCRIPT_DIR/cleanup.sh" "$JIBRI_DIR/"
chmod +x "$JIBRI_DIR/finalize.sh"
chmod +x "$JIBRI_DIR/cleanup.sh"
log_info "Files copied to $JIBRI_DIR"

# Step 3: Update docker-compose.yml with generated password
log_info "Step 3: Configuring Jibri password..."
sed -i "s/CHANGE_ME_SECURE_PASSWORD/$JIBRI_PASSWORD/g" "$JIBRI_DIR/docker-compose.yml"
log_info "Jibri password configured"

# Step 4: Create Prosody users
log_info "Step 4: Creating Prosody users..."
echo ""
log_warn "Run the following commands manually:"
echo "  prosodyctl register jibri auth.meet.mychess.fr $JIBRI_PASSWORD"
echo "  prosodyctl register recorder recorder.meet.mychess.fr $RECORDER_PASSWORD"
echo ""
read -p "Press Enter after running the prosodyctl commands..."

# Step 5: Show manual configuration steps
log_info "Step 5: Manual configuration required"
echo ""
log_warn "Add the following to /etc/prosody/conf.avail/meet.mychess.fr.cfg.lua:"
echo "---"
cat "$SCRIPT_DIR/prosody-jibri.cfg.lua"
echo "---"
echo ""
read -p "Press Enter after updating Prosody config..."

log_warn "Add the following to /etc/jitsi/jicofo/jicofo.conf (inside the jicofo { } block):"
echo "---"
cat "$SCRIPT_DIR/jicofo.conf"
echo "---"
echo ""
read -p "Press Enter after updating Jicofo config..."

log_warn "Add the following to /etc/jitsi/meet/meet.mychess.fr-config.js:"
echo "---"
cat "$SCRIPT_DIR/jitsi-meet-config.js"
echo "---"
echo ""
read -p "Press Enter after updating Jitsi Meet config..."

log_warn "Add the following to /etc/nginx/sites-available/meet.mychess.fr:"
echo "---"
cat "$SCRIPT_DIR/nginx-recordings.conf"
echo "---"
echo ""
read -p "Press Enter after updating Nginx config..."

# Step 6: Restart services
log_info "Step 6: Restarting Jitsi services..."
systemctl restart prosody || log_warn "Failed to restart prosody"
systemctl restart jicofo || log_warn "Failed to restart jicofo"
systemctl restart jitsi-videobridge2 || log_warn "Failed to restart jitsi-videobridge2"
nginx -t && systemctl reload nginx || log_warn "Failed to reload nginx"
log_info "Jitsi services restarted"

# Step 7: Start Jibri
log_info "Step 7: Starting Jibri container..."
cd "$JIBRI_DIR"
docker compose pull
docker compose up -d
log_info "Jibri container started"

# Step 8: Set up cleanup cron
log_info "Step 8: Setting up cleanup cron job..."
CRON_LINE="0 3 * * * $JIBRI_DIR/cleanup.sh"
(crontab -l 2>/dev/null | grep -v "cleanup.sh"; echo "$CRON_LINE") | crontab -
log_info "Cleanup cron job added (runs daily at 3 AM)"

# Step 9: Verification
log_info "Step 9: Verification..."
sleep 10  # Wait for Jibri to start
echo ""
log_info "Checking Jibri status..."
docker logs jibri --tail 20
echo ""

log_info "=== Setup Complete ==="
echo ""
log_info "Generated credentials (save these!):"
echo "  JIBRI_XMPP_PASSWORD: $JIBRI_PASSWORD"
echo "  RECORDER_PASSWORD: $RECORDER_PASSWORD"
echo ""
log_info "To verify Jibri is registered, check Jicofo logs:"
echo "  tail -f /var/log/jitsi/jicofo.log | grep -i jibri"
echo ""
log_info "To test recording:"
echo "  1. Start a video call as a teacher (moderator)"
echo "  2. Click the 'Record' button in Jitsi toolbar"
echo "  3. Speak for 30 seconds, then stop recording"
echo "  4. Check $RECORDINGS_DIR for the MP4 file"
echo "  5. Verify the webhook was called in backend logs"
echo ""
