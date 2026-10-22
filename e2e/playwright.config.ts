import { defineConfig, devices } from '@playwright/test';

/**
 * Two layers, one tool:
 *  - mocked:   the Angular app against an in-browser fake backend (page.route). No services,
 *              no data, no flakiness from environments. Runs on every PR in ~1 minute.
 *  - journeys: the 3 business journeys against a real deployed stack (BASE_URL).
 *              Happy paths only; edge cases live in lower, faster layers.
 */
const isCI = !!process.env.CI;
const executablePath = process.env.CHROMIUM_PATH || undefined; // local override only

export default defineConfig({
  testDir: './tests',
  fullyParallel: true,
  forbidOnly: isCI,
  // One retry in CI, and every retry is reported: a test that needed it is "flaky", not "green".
  retries: isCI ? 1 : 0,
  reporter: isCI ? [['list'], ['html', { open: 'never' }], ['junit', { outputFile: 'results/junit.xml' }]] : 'list',
  use: {
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: executablePath ? 'off' : 'retain-on-failure',
    testIdAttribute: 'data-testid',
    launchOptions: { executablePath },
  },
  projects: [
    {
      name: 'mocked',
      testDir: './tests/mocked',
      use: { ...devices['Desktop Chrome'], baseURL: 'http://localhost:4200' },
    },
    {
      name: 'journeys',
      testDir: './tests/journeys',
      timeout: 60_000,
      use: { ...devices['Desktop Chrome'], baseURL: process.env.BASE_URL ?? 'http://localhost:8080' },
    },
  ],
  // The mocked project starts the Angular dev server itself; journeys expect a deployed stack.
  webServer: process.argv.includes('--project=journeys')
    ? undefined
    : {
        command: 'npm --prefix ../frontend start -- --port 4200',
        url: 'http://localhost:4200',
        reuseExistingServer: !isCI,
        timeout: 180_000,
      },
});
