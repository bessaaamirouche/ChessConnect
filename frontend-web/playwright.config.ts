import { defineConfig, devices } from '@playwright/test';

/**
 * Configuration Playwright pour les tests E2E de mychess
 * Exécuter avec: npx playwright test
 */
export default defineConfig({
  testDir: './tests',

  /* Timeout pour chaque test */
  timeout: 60000,

  /* Timeout pour les assertions expect */
  expect: {
    timeout: 10000,
  },

  /* Run tests in files in parallel */
  fullyParallel: true,

  /* Fail the build on CI if you accidentally left test.only in the source code. */
  forbidOnly: !!process.env.CI,

  /* Retry on CI only */
  retries: process.env.CI ? 2 : 1,

  /* Opt out of parallel tests on CI. */
  workers: process.env.CI ? 1 : 2,

  /* Reporter to use. See https://playwright.dev/docs/test-reporters */
  reporter: [
    ['html', { open: 'never' }],
    ['list'],
  ],

  /* Shared settings for all the projects below. */
  use: {
    /* Base URL - Docker local */
    baseURL: process.env.BASE_URL || 'http://localhost:4200',

    /* Collect trace when retrying the failed test. */
    trace: 'on-first-retry',

    /* Screenshot on failure */
    screenshot: 'only-on-failure',

    /* Video on failure */
    video: 'retain-on-failure',

    /* Locale français */
    locale: 'fr-FR',

    /* Timezone Paris */
    timezoneId: 'Europe/Paris',
  },

  /* Configure projects for major browsers */
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },

    /* Mobile viewport (using Chromium) */
    {
      name: 'mobile',
      use: {
        ...devices['Pixel 5'],
        // Force Chromium instead of webkit
      },
    },
  ],

  /* Output folder for test artifacts */
  outputDir: 'test-results/',
});
