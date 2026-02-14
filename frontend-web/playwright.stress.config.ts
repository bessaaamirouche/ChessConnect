import { defineConfig, devices } from '@playwright/test';

/**
 * Configuration Playwright pour les tests de stress mychess
 *
 * Execution:
 *   npx playwright test --config=playwright.stress.config.ts
 *
 * Avec URL custom:
 *   BASE_URL=https://mychess.fr npx playwright test --config=playwright.stress.config.ts
 */
export default defineConfig({
  testDir: './tests/stress',

  /* Timeout long pour les flows multi-etapes + 10min d'appel video + attente webhook */
  timeout: 1_800_000,

  /* Timeout pour les assertions expect */
  expect: {
    timeout: 15_000,
  },

  fullyParallel: false,

  /* Pas de retry en stress test */
  retries: 0,

  /* Un seul worker: le test orchestre tout en interne via Promise.all */
  workers: 1,

  reporter: [
    ['list'],
    ['html', { open: 'never' }],
  ],

  use: {
    baseURL: process.env['BASE_URL'] || 'https://mychess.fr',

    /* Traces et videos desactives pour les perfs (activer pour debug) */
    trace: 'off',
    screenshot: 'only-on-failure',
    video: 'off',

    /* Locale francais */
    locale: 'fr-FR',
    timezoneId: 'Europe/Paris',

    /* Headless pour les performances */
    headless: true,

    /* Fake media streams + WebRTC flags pour les appels video Jitsi */
    launchOptions: {
      args: [
        '--use-fake-device-for-media-stream',
        '--use-fake-ui-for-media-stream',
        '--no-sandbox',
        '--disable-setuid-sandbox',
        '--disable-dev-shm-usage',
        '--disable-gpu',
        '--disable-extensions',
        '--disable-web-security',
        '--allow-running-insecure-content',
        '--autoplay-policy=no-user-gesture-required',
        '--enable-features=WebRtcHideLocalIpsWithMdns',
        '--ignore-certificate-errors',
        '--ignore-certificate-errors-spki-list',
        '--disable-features=IsolateOrigins,site-per-process',
      ],
    },
  },

  projects: [
    {
      name: 'stress-chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],

  outputDir: 'test-results/stress/',
});
