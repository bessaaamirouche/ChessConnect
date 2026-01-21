-- Prosody Jibri Configuration
-- Add this to /etc/prosody/conf.avail/meet.mychess.fr.cfg.lua
-- After adding, run: prosodyctl register jibri auth.meet.mychess.fr YOUR_JIBRI_PASSWORD
--                    prosodyctl register recorder recorder.meet.mychess.fr YOUR_RECORDER_PASSWORD

-- =============================================================================
-- ADD THE FOLLOWING TO YOUR EXISTING meet.mychess.fr.cfg.lua FILE
-- =============================================================================

-- Virtual host for recorder (Jibri)
VirtualHost "recorder.meet.mychess.fr"
    modules_enabled = {
        "ping";
    }
    authentication = "internal_plain"

-- Internal MUC component for Jibri communication
Component "internal.auth.meet.mychess.fr" "muc"
    storage = "memory"
    modules_enabled = {
        "ping";
    }
    admins = {
        "focus@auth.meet.mychess.fr",
        "jibri@auth.meet.mychess.fr"
    }
    muc_room_locking = false
    muc_room_default_public_jids = true

-- =============================================================================
-- ALSO UPDATE YOUR MAIN VirtualHost TO ENABLE RECORDING
-- Add "muc_lobby_rooms" if not already present for Jibri support
-- =============================================================================

-- In your main VirtualHost "meet.mychess.fr" section, ensure you have:
-- modules_enabled = {
--     ...
--     "muc_lobby_rooms";  -- Required for Jibri
--     ...
-- }
