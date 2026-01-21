// Jitsi Meet Recording Configuration
// Add/update these values in /etc/jitsi/meet/meet.mychess.fr-config.js

var config = {
    // ... existing config ...

    // Enable file recording with Jibri
    fileRecordingsEnabled: true,

    // Recording service configuration
    recordingService: {
        enabled: true,
        sharingEnabled: false  // Don't show sharing options
    },

    // Local recording (fallback, browser-based)
    localRecording: {
        enabled: true,
        format: 'mp4'
    },

    // Enable features based on JWT token
    enableFeaturesBasedOnToken: true,

    // Hide the recording button from non-moderators (controlled by JWT)
    // This ensures only teachers (moderators) can start recordings
    toolbarButtons: [
        'camera',
        'chat',
        'closedcaptions',
        'desktop',
        'fullscreen',
        'fodeviceselection',
        'hangup',
        'microphone',
        'participants-pane',
        'profile',
        'raisehand',
        'recording',  // Only visible to moderators via JWT
        'security',
        'select-background',
        'settings',
        'tileview',
        'videoquality'
    ],

    // ... rest of existing config ...
};
