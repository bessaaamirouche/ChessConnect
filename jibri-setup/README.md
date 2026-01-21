# Jibri Setup for mychess.fr Video Recording

This directory contains all configuration files and scripts needed to set up Jibri (Jitsi Broadcasting Infrastructure) for automatic video recording of chess lessons.

## Prerequisites

- Jitsi Meet already installed and running on meet.mychess.fr
- Docker and Docker Compose installed on the server
- Root access to the server
- At least 500 GB of disk space for recordings

## Quick Start

```bash
# 1. Copy this directory to the Jitsi server
scp -r jibri-setup/ root@meet.mychess.fr:/tmp/

# 2. SSH to the server
ssh root@meet.mychess.fr

# 3. Run the setup script
cd /tmp/jibri-setup
chmod +x setup.sh
./setup.sh
```

## Files Description

| File | Description |
|------|-------------|
| `docker-compose.yml` | Docker configuration for Jibri container |
| `finalize.sh` | Script called by Jibri when recording ends - sends webhook to backend |
| `cleanup.sh` | Cron script to delete recordings older than 90 days |
| `prosody-jibri.cfg.lua` | Prosody configuration additions for Jibri |
| `jicofo.conf` | Jicofo configuration for Jibri brewery |
| `jitsi-meet-config.js` | Jitsi Meet frontend config for recording feature |
| `nginx-recordings.conf` | Nginx config to serve recorded MP4 files |
| `setup.sh` | Master setup script (run this) |

## Manual Installation

If you prefer manual installation:

### 1. Create Directories

```bash
mkdir -p /opt/jibri
mkdir -p /var/jibri/recordings
mkdir -p /var/log/jibri
chmod 777 /var/jibri/recordings
```

### 2. Copy Files

```bash
cp docker-compose.yml finalize.sh cleanup.sh /opt/jibri/
chmod +x /opt/jibri/finalize.sh /opt/jibri/cleanup.sh
```

### 3. Generate Passwords

```bash
JIBRI_PASSWORD=$(openssl rand -hex 16)
RECORDER_PASSWORD=$(openssl rand -hex 16)
echo "Jibri: $JIBRI_PASSWORD"
echo "Recorder: $RECORDER_PASSWORD"
```

### 4. Update docker-compose.yml

Replace `CHANGE_ME_SECURE_PASSWORD` with your generated password.

### 5. Create Prosody Users

```bash
prosodyctl register jibri auth.meet.mychess.fr YOUR_JIBRI_PASSWORD
prosodyctl register recorder recorder.meet.mychess.fr YOUR_RECORDER_PASSWORD
```

### 6. Update Configuration Files

Add the contents of:
- `prosody-jibri.cfg.lua` to `/etc/prosody/conf.avail/meet.mychess.fr.cfg.lua`
- `jicofo.conf` to `/etc/jitsi/jicofo/jicofo.conf`
- `jitsi-meet-config.js` to `/etc/jitsi/meet/meet.mychess.fr-config.js`
- `nginx-recordings.conf` to `/etc/nginx/sites-available/meet.mychess.fr`

### 7. Restart Services

```bash
systemctl restart prosody
systemctl restart jicofo
systemctl restart jitsi-videobridge2
nginx -t && systemctl reload nginx
```

### 8. Start Jibri

```bash
cd /opt/jibri
docker compose up -d
```

### 9. Setup Cleanup Cron

```bash
echo "0 3 * * * /opt/jibri/cleanup.sh" | crontab -
```

## Verification

### Check if Jibri is registered

```bash
# In Jicofo logs, look for "Jibri registered"
tail -f /var/log/jitsi/jicofo.log | grep -i jibri
```

### Check Jibri container logs

```bash
docker logs jibri -f
```

### Test Recording

1. Create a test lesson with ID 999 in the database
2. Join `mychess-lesson-999` as a teacher (moderator)
3. Click the "Record" button in Jitsi toolbar
4. Speak for 30 seconds
5. Stop the recording
6. Check for the file: `ls -la /var/jibri/recordings/mychess-lesson-999/`
7. Verify webhook was called in backend logs

## How It Works

1. **Teacher starts recording**: The teacher (JWT with `moderator: true`) clicks "Record" in Jitsi
2. **Jibri captures**: Jibri runs a Chrome instance that joins the call and captures audio/video
3. **Recording ends**: When the teacher stops recording, Jibri saves the MP4 file
4. **Finalize script**: Jibri calls `finalize.sh` with the recording directory path
5. **Webhook call**: The script extracts the lesson ID from room name and calls the backend webhook
6. **Database update**: The backend updates the lesson's `recordingUrl` field
7. **Student playback**: The student can see and play the recording in their lesson history

## Storage Estimation

- 1 hour lesson ≈ 500 MB - 1 GB (720p)
- 10 lessons/day ≈ 5-10 GB/day
- 1 month ≈ 150-300 GB

Ensure at least **500 GB** of disk space.

## Troubleshooting

### Jibri not registering

```bash
# Check Prosody users exist
prosodyctl check
prosodyctl mod_roster jibri@auth.meet.mychess.fr

# Check XMPP connection
docker logs jibri | grep -i "xmpp\|connection\|error"
```

### Recording button not visible

- Verify the user is a teacher/moderator (check JWT claims)
- Check `enableFeaturesBasedOnToken: true` in Jitsi config
- Clear browser cache and rejoin the call

### Recording fails to start

```bash
# Check Jibri status
docker logs jibri --tail 100

# Check if Jibri is busy with another recording
docker exec jibri cat /config/jibri.conf
```

### Webhook not called

```bash
# Check finalize.sh logs
cat /var/log/jibri/finalize.log

# Test webhook manually
curl -X POST -H "Content-Type: application/json" \
  -d '{"filename":"test.mp4","path":"/var/jibri/recordings/test/test.mp4","room":"mychess-lesson-1"}' \
  https://api.mychess.fr/api/recordings/webhook
```

### Video not playing

- Check CORS headers in Nginx config
- Verify the file exists: `ls -la /var/jibri/recordings/`
- Check file permissions: `chmod 644 /var/jibri/recordings/*/*.mp4`

## Architecture

```
┌─────────────────┐
│   Teacher       │
│   (Moderator)   │
└───────┬─────────┘
        │ Clicks "Record"
        ▼
┌─────────────────┐
│   Jitsi Meet    │
│   (Frontend)    │
└───────┬─────────┘
        │ Recording request
        ▼
┌─────────────────┐      ┌─────────────────┐
│    Jicofo       │─────▶│     Jibri       │
│                 │      │  (Docker)       │
└─────────────────┘      └───────┬─────────┘
                                 │ Recording ends
                                 ▼
                         ┌─────────────────┐
                         │  finalize.sh    │
                         └───────┬─────────┘
                                 │ Webhook POST
                                 ▼
┌─────────────────┐      ┌─────────────────┐
│    Nginx        │◀─────│    Backend      │
│ /recordings/*   │      │  (Spring Boot)  │
└─────────────────┘      └─────────────────┘
        │                        │
        │ MP4 stream             │ Update recordingUrl
        ▼                        ▼
┌─────────────────┐      ┌─────────────────┐
│    Student      │      │   PostgreSQL    │
│   (Frontend)    │      │                 │
└─────────────────┘      └─────────────────┘
```
