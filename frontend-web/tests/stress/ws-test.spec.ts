import { test } from '@playwright/test';
import { execSync } from 'child_process';

test('Jitsi IFrame API - check Prosody connections', async ({ page }) => {
  page.on('console', msg => {
    const text = msg.text();
    if (text.includes('[Jitsi]') || text.includes('error') || text.includes('Error')) {
      console.log(`  [CONSOLE] ${text.substring(0, 200)}`);
    }
  });

  // Get initial Prosody log line count
  const initialLines = execSync('wc -l < /var/log/prosody/prosody.log').toString().trim();
  console.log(`[TEST] Prosody log lines before: ${initialLines}`);

  // Navigate to real mychess.fr and inject Jitsi IFrame
  console.log('[TEST] Navigating to mychess.fr...');
  await page.goto('https://mychess.fr', { waitUntil: 'load' });

  console.log('[TEST] Injecting Jitsi IFrame API...');
  await page.evaluate(() => {
    return new Promise<void>((resolve) => {
      const script = document.createElement('script');
      script.src = 'https://meet.mychess.fr/external_api.js';
      script.onload = () => {
        console.log('[Jitsi] external_api.js loaded');
        const container = document.createElement('div');
        container.id = 'jitsi-container';
        container.style.cssText = 'width:800px;height:600px;position:fixed;top:0;left:0;z-index:9999;';
        document.body.appendChild(container);

        const api = new (window as any).JitsiMeetExternalAPI('meet.mychess.fr', {
          roomName: `test-prosody-${Date.now()}`,
          parentNode: container,
          configOverwrite: {
            prejoinPageEnabled: false,
            startWithAudioMuted: true,
            startWithVideoMuted: true,
            disableDeepLinking: true,
          },
          interfaceConfigOverwrite: {
            TOOLBAR_BUTTONS: [],
          },
        });
        api.addListener('videoConferenceJoined', (data: any) => {
          console.log('[Jitsi] CONFERENCE JOINED: ' + JSON.stringify(data));
        });
        console.log('[Jitsi] IFrame API created');
        resolve();
      };
      document.head.appendChild(script);
    });
  });

  console.log('[TEST] Waiting 30s for Jitsi to connect...');
  await page.waitForTimeout(30000);

  // Check Prosody log for new connections
  const finalLines = execSync('wc -l < /var/log/prosody/prosody.log').toString().trim();
  console.log(`[TEST] Prosody log lines after: ${finalLines}`);
  const newLines = parseInt(finalLines) - parseInt(initialLines);
  if (newLines > 0) {
    const logs = execSync(`tail -${newLines} /var/log/prosody/prosody.log`).toString();
    console.log(`[TEST] NEW Prosody entries (${newLines} lines):`);
    console.log(logs);
  } else {
    console.log('[TEST] NO new Prosody connections!');
  }

  // Also check nginx for WebSocket/BOSH
  const wsLogs = execSync('grep "xmpp-websocket\\|http-bind" /var/log/nginx/access.log | tail -5 2>/dev/null || true').toString();
  console.log(`[TEST] Recent WebSocket/BOSH nginx entries:\n${wsLogs}`);
});
