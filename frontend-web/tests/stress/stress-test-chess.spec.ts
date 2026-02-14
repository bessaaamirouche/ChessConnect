import { test, Browser, BrowserContext, Page } from '@playwright/test';
import { execSync } from 'child_process';

// ============================================================================
// CONFIGURATION
// ============================================================================

const API_BASE = '/api';
const PASSWORD = 'StressTest2026!';
const CALL_DURATION_MS = 3 * 60_000; // 3 minutes in call for recording
const PHASE_DELAY_MS = 3_000;
const BATCH_SIZE = 5;
const LOGIN_BATCH_SIZE = 4; // Smaller batches for login to avoid Chrome resource contention
const LOGIN_INTER_BATCH_DELAY_MS = 10_000; // 10s between login batches — avoids browser overload with 20 contexts

// Docker DB access
const DB_CONTAINER = process.env['DB_CONTAINER'] || 'mychess-db';
const DB_USER = process.env['DB_USER'] || 'chess';
const DB_NAME = process.env['DB_NAME'] || 'chessconnect';

// ============================================================================
// TYPES
// ============================================================================

interface TestUser {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  role: 'STUDENT' | 'TEACHER';
  hourlyRateCents?: number;
  bio?: string;
  languages?: string[];
  birthDate?: string;
  context?: BrowserContext;
  page?: Page;
  userId?: number;
  teacherId?: number;
  lessonId?: number;
  invitationToken?: string;
}

interface PhaseResult {
  name: string;
  success: number;
  total: number;
  failures: string[];
  durationMs: number;
}

// ============================================================================
// USER GENERATION — 20 utilisateurs (10 coachs + 10 eleves)
// ============================================================================

// 10 individual pairs = 10 coaches + 10 students = 20 users, 10 parallel calls
const COACH_FIRST_NAMES = ['Alexandre', 'Baptiste', 'Clement', 'Damien', 'Etienne', 'Fabien', 'Guillaume', 'Hugo', 'Ismael', 'Julien'];
const STUDENT_FIRST_NAMES = ['Alice', 'Beatrice', 'Camille', 'Diane', 'Emma', 'Fiona', 'Gabrielle', 'Helene', 'Ines', 'Julie'];

function generateUsers(): {
  coaches: TestUser[];
  students: TestUser[];
} {
  const ts = Date.now();

  const coaches: TestUser[] = COACH_FIRST_NAMES.map((name, i) => ({
    email: `coach_ind_${i + 1}_${ts}@stress.mychess.fr`,
    password: PASSWORD,
    firstName: name,
    lastName: 'Testcoach',
    role: 'TEACHER' as const,
    hourlyRateCents: 3000,
    bio: 'Coach de stress test avec plus de dix ans experience en enseignement des echecs.',
    languages: ['FR'],
  }));

  const students: TestUser[] = STUDENT_FIRST_NAMES.map((name, i) => ({
    email: `student_ind_${i + 1}_${ts}@stress.mychess.fr`,
    password: PASSWORD,
    firstName: name,
    lastName: 'Testeleve',
    role: 'STUDENT' as const,
    birthDate: '2000-01-15',
  }));

  return { coaches, students };
}

// ============================================================================
// DB HELPERS — via docker exec
// ============================================================================

function runSQL(sql: string): string {
  try {
    return execSync(
      `docker exec ${DB_CONTAINER} psql -U ${DB_USER} -d ${DB_NAME} -t -A -c "${sql.replace(/"/g, '\\"')}"`,
      { encoding: 'utf-8', timeout: 10_000 }
    ).trim();
  } catch (e: any) {
    console.error(`  [SQL ERROR] ${sql.substring(0, 120)}...`, e.message?.substring(0, 100));
    return '';
  }
}

/** Marque tous les users stress test comme email verifie */
function verifyAllStressEmails(): void {
  runSQL("UPDATE users SET email_verified = true WHERE email LIKE '%@stress.mychess.fr'");
  log('DB', 'Tous les emails de stress test marques comme verifies');
}

/** Cree un wallet et credite 1000EUR pour chaque student stress test */
function fundAllStudentWallets(): void {
  runSQL(`
    INSERT INTO student_wallets (user_id, balance_cents, total_top_ups_cents, total_used_cents, total_refunded_cents, created_at, updated_at)
    SELECT id, 0, 0, 0, 0, NOW(), NOW() FROM users
    WHERE email LIKE '%student%@stress.mychess.fr'
    AND id NOT IN (SELECT user_id FROM student_wallets)
  `);

  runSQL(`
    UPDATE student_wallets SET balance_cents = 100000, total_top_ups_cents = 100000, updated_at = NOW()
    WHERE user_id IN (SELECT id FROM users WHERE email LIKE '%student%@stress.mychess.fr')
  `);

  runSQL(`
    INSERT INTO credit_transactions (user_id, transaction_type, amount_cents, description, created_at)
    SELECT id, 'TOPUP', 100000, 'Stress test funding', NOW()
    FROM users WHERE email LIKE '%student%@stress.mychess.fr'
    AND id NOT IN (
      SELECT user_id FROM credit_transactions WHERE description = 'Stress test funding'
    )
  `);

  log('DB', 'Wallets etudiants credites de 1000EUR');
}

/** Cree un abonnement Premium actif pour chaque student stress test (requis pour l'enregistrement video) */
function grantAllStudentsPremium(): void {
  runSQL(`
    INSERT INTO subscriptions (student_id, plan_type, price_cents, start_date, is_active, created_at)
    SELECT id, 'PREMIUM', 499, CURRENT_DATE, true, NOW()
    FROM users
    WHERE email LIKE '%student%@stress.mychess.fr'
    AND id NOT IN (SELECT student_id FROM subscriptions WHERE is_active = true)
  `);
  log('DB', 'Abonnements Premium actifs crees pour tous les etudiants');
}

/** Marque tous les cours stress test confirmes comme COMPLETED */
function completeAllStressLessons(): void {
  const result = runSQL(`
    UPDATE lessons SET status = 'COMPLETED'
    WHERE status = 'CONFIRMED'
    AND id IN (
      SELECT l.id FROM lessons l
      JOIN users u ON (l.student_id = u.id OR l.teacher_id = u.id)
      WHERE u.email LIKE '%@stress.mychess.fr'
    )
  `);
  log('DB', 'Cours stress test marques comme COMPLETED');
}

/** Recupere le user ID par email */
function getUserIdByEmail(email: string): number | null {
  const result = runSQL(`SELECT id FROM users WHERE email = '${email}'`);
  return result ? parseInt(result, 10) : null;
}

/**
 * Apres confirmation, met a jour scheduled_at a NOW()+5min et s'assure que zoom_link est set.
 * Cela rend le bouton "Rejoindre" visible immediatement (canJoinLesson: now >= scheduledAt - 15min).
 * On attendra ensuite ~3min pour que le cours demarre reellement.
 *
 * IMPORTANT: La JVM Spring Boot est en CET (Europe/Paris) et utilise LocalDateTime.now(),
 * mais PostgreSQL NOW() retourne UTC. On utilise NOW() AT TIME ZONE 'Europe/Paris'
 * pour stocker un timestamp que Java interpretera correctement.
 */
function makeAllStressLessonsJoinable(): void {
  // Update scheduled_at to 5 minutes from now IN THE JVM TIMEZONE (CET)
  // DB stores "timestamp without time zone", Java reads it as CET via LocalDateTime
  const updated = runSQL(`
    UPDATE lessons SET scheduled_at = (NOW() AT TIME ZONE 'Europe/Paris') + INTERVAL '5 minutes'
    WHERE id IN (
      SELECT l.id FROM lessons l
      JOIN users u ON (l.student_id = u.id OR l.teacher_id = u.id)
      WHERE u.email LIKE '%@stress.mychess.fr'
      AND l.status = 'CONFIRMED'
    )
  `);
  log('DB', `scheduled_at mis a CET NOW()+5min pour les cours stress test`);

  // Ensure zoom_link is set (should be auto-set on confirm, but just in case)
  runSQL(`
    UPDATE lessons SET zoom_link = 'https://meet.mychess.fr/mychess-lesson-' || id
    WHERE zoom_link IS NULL
    AND id IN (
      SELECT l.id FROM lessons l
      JOIN users u ON (l.student_id = u.id OR l.teacher_id = u.id)
      WHERE u.email LIKE '%@stress.mychess.fr'
      AND l.status = 'CONFIRMED'
    )
  `);
  log('DB', 'zoom_link verifie/set pour tous les cours confirmes');
}

/** Verifie si les enregistrements video existent en DB */
function checkRecordingsInDB(): { total: number; withRecording: number; withSegments: number; details: string[] } {
  const rows = runSQL(`
    SELECT l.id, l.status, l.recording_url, l.recording_segments, l.thumbnail_url
    FROM lessons l
    JOIN users u ON (l.student_id = u.id OR l.teacher_id = u.id)
    WHERE u.email LIKE '%@stress.mychess.fr'
    AND l.status IN ('CONFIRMED', 'COMPLETED')
    GROUP BY l.id, l.status, l.recording_url, l.recording_segments, l.thumbnail_url
  `);

  const details: string[] = [];
  let total = 0;
  let withRecording = 0;
  let withSegments = 0;

  if (rows) {
    for (const row of rows.split('\n').filter(r => r.trim())) {
      const parts = row.split('|');
      total++;
      const lessonId = parts[0];
      const status = parts[1] || '';
      const recordingUrl = parts[2] || '';
      const segments = parts[3] || '';
      const thumbnail = parts[4] || '';

      if (recordingUrl) withRecording++;
      if (segments && segments !== '[]' && segments !== 'null') withSegments++;

      details.push(`  Lesson #${lessonId} (${status}): recording=${recordingUrl ? 'YES' : 'NO'}, segments=${segments ? 'YES' : 'NO'}, thumbnail=${thumbnail ? 'YES' : 'NO'}`);
    }
  }

  return { total, withRecording, withSegments, details };
}

// ============================================================================
// HELPERS
// ============================================================================

function sleep(ms: number): Promise<void> {
  return new Promise(resolve => setTimeout(resolve, ms));
}

function log(phase: string, msg: string): void {
  const ts = new Date().toISOString().substring(11, 19);
  console.log(`  [${ts}] [${phase}] ${msg}`);
}

async function safeRun<T>(label: string, phase: string, fn: () => Promise<T>): Promise<T | null> {
  const start = Date.now();
  try {
    const result = await fn();
    log(phase, `\u2713 ${label} (${Date.now() - start}ms)`);
    return result;
  } catch (error: any) {
    log(phase, `\u2717 ${label}: ${error.message?.substring(0, 200) || error}`);
    return null;
  }
}

/** Execute tasks in batches with optional delay between batches */
async function runInBatches<T>(
  items: T[],
  batchSize: number,
  fn: (item: T) => Promise<any>,
  interBatchDelay = 0
): Promise<(any)[]> {
  const results: any[] = [];
  for (let i = 0; i < items.length; i += batchSize) {
    if (i > 0 && interBatchDelay > 0) {
      await sleep(interBatchDelay);
    }
    const batch = items.slice(i, i + batchSize);
    const batchResults = await Promise.all(batch.map(fn));
    results.push(...batchResults);
  }
  return results;
}

/** Ferme le bandeau cookies s'il est present */
async function dismissCookieBanner(page: Page): Promise<void> {
  try {
    const acceptBtn = page.locator('button:has-text("Accepter")');
    if (await acceptBtn.isVisible({ timeout: 2_000 })) {
      await acceptBtn.click();
      await page.waitForTimeout(300);
    }
  } catch {
    // Pas de bandeau cookies
  }
}

/** Capture un screenshot pour debug (non-bloquant) */
async function debugScreenshot(page: Page, name: string): Promise<void> {
  try {
    await page.screenshot({ path: `test-results/stress/debug-${name}-${Date.now()}.png`, fullPage: true });
  } catch {
    // Ignore
  }
}

// ============================================================================
// PLAYWRIGHT UI FLOWS
// ============================================================================

async function registerUser(page: Page, user: TestUser): Promise<void> {
  await page.goto('/register');
  await page.waitForSelector('.role-btn', { state: 'visible', timeout: 15_000 });
  await dismissCookieBanner(page);

  if (user.role === 'STUDENT') {
    await page.locator('.role-btn').first().click();
  } else {
    await page.locator('.role-btn').nth(1).click();
  }

  await page.fill('#firstName', user.firstName);
  await page.fill('#lastName', user.lastName);
  await page.fill('#email', user.email);
  await page.fill('#password', user.password);

  if (user.role === 'TEACHER') {
    await page.fill('#hourlyRate', String((user.hourlyRateCents || 3000) / 100));
    await page.fill('#bio', user.bio || 'Coach de test avec experience.');
    const frBtn = page.locator('.language-btn').first();
    const isActive = await frBtn.evaluate(el => el.classList.contains('language-btn--active'));
    if (!isActive) await frBtn.click();
  } else {
    const dateInputs = page.locator('app-date-input input');
    await dateInputs.nth(0).fill('15');
    await dateInputs.nth(1).fill('01');
    await dateInputs.nth(2).fill('2000');
  }

  const submitBtn = page.locator('button[type="submit"].btn--primary');
  await submitBtn.scrollIntoViewIfNeeded();
  await submitBtn.click({ force: true });

  await page.waitForSelector('.register-success, .alert-inline--visible', { timeout: 20_000 });
}

async function loginUser(page: Page, user: TestUser): Promise<void> {
  await page.goto('/login');
  await page.waitForSelector('#email', { state: 'visible', timeout: 15_000 });
  await dismissCookieBanner(page);

  await page.fill('#email', user.email);
  await page.fill('#password', user.password);
  await page.locator('button[type="submit"].btn--primary').click({ force: true });

  await page.waitForURL(url => {
    const path = new URL(url).pathname;
    return !path.includes('/login') && !path.includes('/register');
  }, { timeout: 45_000 });
}

async function createAvailability(page: Page, lessonType: 'INDIVIDUAL' | 'GROUP', slotIndex: number): Promise<void> {
  await page.goto('/availability');
  await page.waitForLoadState('domcontentloaded');
  await dismissCookieBanner(page);

  await page.waitForSelector('button.btn--primary, .availability-page', { timeout: 15_000 });

  const addBtn = page.locator('button.btn--primary').filter({ hasText: /ajouter|addSlot|add/i });
  const target = (await addBtn.count()) > 0
    ? addBtn.first()
    : page.locator('.page-header button.btn--primary, button.btn--primary').first();
  await target.click();
  await page.waitForSelector('.modal-overlay', { state: 'visible', timeout: 10_000 });

  // Lesson type
  await page.locator(`input[name="lessonType"][value="${lessonType}"]`).click();

  // Day of week (spread across days to avoid conflicts)
  const days = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];
  await page.selectOption('#daySelect', days[slotIndex % days.length]);

  // Start hour (stagger: 08:00, 09:00, 10:00, ...)
  const startHour = (8 + slotIndex) % 24;
  const timeSelects = page.locator('.time-picker__select');
  await timeSelects.first().selectOption(startHour.toString().padStart(2, '0'));
  await timeSelects.nth(1).selectOption('00');

  await page.locator('.modal__footer .btn--primary').click();
  await page.waitForSelector('.modal-overlay', { state: 'hidden', timeout: 10_000 });
}

async function bookIndividualLesson(page: Page, teacherId: number): Promise<void> {
  await page.goto(`/book/${teacherId}`);
  await dismissCookieBanner(page);

  await page.waitForSelector('.slots-calendar, .slots-empty', { timeout: 25_000 });

  const hasSlots = await page.locator('.slot-btn--available').count();
  if (hasSlots === 0) {
    await debugScreenshot(page, 'no-slots-individual');
    throw new Error('Aucun creneau disponible');
  }

  await page.locator('.slot-btn--available').first().click();
  await page.waitForSelector('.selected-slot-info__value', { state: 'visible', timeout: 5_000 });

  // Enable wallet payment
  const walletToggle = page.locator('.wallet-compact__toggle input[type="checkbox"]');
  if ((await walletToggle.count()) > 0 && !(await walletToggle.isChecked())) {
    await walletToggle.click();
  }

  await page.locator('form.booking-form button[type="submit"].btn--primary').click();

  // Handle wallet confirmation modal ("Confirmer la création du cours — X€ seront débités")
  const confirmModal = page.locator('button:has-text("Confirmer")');
  try {
    await confirmModal.waitFor({ state: 'visible', timeout: 5_000 });
    await confirmModal.click();
  } catch {
    // No confirmation modal — direct flow (e.g. Stripe or free trial)
  }

  // After wallet confirmation, the app may show .success-card OR redirect to dashboard.
  // Both indicate success — wait for either.
  await Promise.race([
    page.waitForSelector('.success-card', { timeout: 30_000 }).catch(() => null),
    page.waitForURL(url => new URL(url).pathname.includes('/dashboard'), { timeout: 30_000 }).catch(() => null),
  ]);

  // Verify booking actually happened by checking we're on success or dashboard
  const currentUrl = page.url();
  const hasSuccessCard = await page.locator('.success-card').count() > 0;
  if (!hasSuccessCard && !currentUrl.includes('/dashboard')) {
    await debugScreenshot(page, 'individual-booking-fail');
    throw new Error('Reservation individuelle: ni success-card ni redirection dashboard');
  }
}

async function bookGroupLesson(page: Page, teacherId: number, groupSize: number): Promise<string> {
  await page.goto(`/book/${teacherId}`);
  await dismissCookieBanner(page);

  // Wait for lesson type buttons
  await page.waitForSelector('.lesson-type-btn', { timeout: 25_000 });

  // Switch to group mode (2nd button)
  await page.locator('.lesson-type-btn').nth(1).click();
  await sleep(500);

  // Select group size
  if (groupSize === 2) {
    await page.locator('.group-size-btn').first().click();
  } else {
    await page.locator('.group-size-btn').nth(1).click();
  }
  await sleep(500);

  // Wait for GROUP slots to load
  await page.waitForSelector('.slots-calendar, .slots-empty', { timeout: 20_000 });

  const hasSlots = await page.locator('.slot-btn--available').count();
  if (hasSlots === 0) {
    await debugScreenshot(page, 'no-slots-group');
    throw new Error('Aucun creneau GROUP disponible');
  }

  await page.locator('.slot-btn--available').first().click();
  await page.waitForSelector('.selected-slot-info__value', { state: 'visible', timeout: 5_000 });

  // Enable wallet
  const walletToggle = page.locator('.wallet-compact__toggle input[type="checkbox"]');
  if ((await walletToggle.count()) > 0 && !(await walletToggle.isChecked())) {
    await walletToggle.click();
  }

  // Submit group booking
  const submitBtn = page.locator('form.booking-form button[type="submit"]');
  await submitBtn.scrollIntoViewIfNeeded();
  await submitBtn.click({ force: true });

  // Handle wallet confirmation modal
  const confirmModal = page.locator('button:has-text("Confirmer")');
  try {
    await confirmModal.waitFor({ state: 'visible', timeout: 5_000 });
    await confirmModal.click();
  } catch {
    // No confirmation modal
  }

  // Wait for success with invitation link — capture screenshot on failure for debug
  try {
    await page.waitForSelector('.success-card', { timeout: 30_000 });
  } catch {
    await debugScreenshot(page, 'group-booking-no-success');
    throw new Error('Group booking: success-card non affichee');
  }

  // Extract invitation token
  const invitationUrlEl = page.locator('.invitation-link-box__url');
  if ((await invitationUrlEl.count()) === 0) {
    await debugScreenshot(page, 'group-booking-no-token');
    throw new Error('Group booking: invitation-link-box__url non trouvee');
  }

  const invitationUrl = await invitationUrlEl.innerText();
  const token = invitationUrl.split('/join/').pop()?.trim();
  if (!token) throw new Error('Token d\'invitation non trouve dans: ' + invitationUrl);

  return token;
}

async function joinGroupLesson(page: Page, token: string): Promise<void> {
  await page.goto(`/join/${token}`);
  await dismissCookieBanner(page);

  await page.waitForSelector('.join-card, .join-card--error', { timeout: 15_000 });

  const isError = await page.locator('.join-card--error').count();
  if (isError > 0) {
    await debugScreenshot(page, 'join-group-error');
    throw new Error('Invitation invalide ou expiree');
  }

  // Click "join with credit" or fallback to primary button
  const creditBtn = page.locator('.btn--gold');
  if ((await creditBtn.count()) > 0) {
    await creditBtn.click();
  } else {
    await page.locator('.action-section .btn--primary').first().click();
  }

  await page.waitForSelector('.join-card--success', { timeout: 20_000 });
}

async function confirmLesson(context: BrowserContext, lessonId: number): Promise<void> {
  const response = await context.request.patch(`${API_BASE}/lessons/${lessonId}/status`, {
    data: { status: 'CONFIRMED' },
  });
  if (!response.ok()) {
    const text = await response.text();
    throw new Error(`Confirm failed (${response.status()}): ${text.substring(0, 100)}`);
  }
}

async function joinVideoCall(page: Page, context: BrowserContext, lessonId: number, isTeacher: boolean): Promise<void> {
  // Capture Jitsi console logs for diagnostics (including recording events)
  const jitsiLogs: string[] = [];
  page.on('console', msg => {
    const text = msg.text();
    if (text.includes('[Jitsi]') || text.includes('location') || text.includes('conference') || text.includes('recording') || text.includes('Recording')) {
      jitsiLogs.push(text);
    }
  });

  // Teacher must mark they joined first
  if (isTeacher) {
    try {
      await context.request.patch(`${API_BASE}/lessons/${lessonId}/teacher-joined`);
    } catch {
      log('VIDEO', `teacher-joined API failed for lesson ${lessonId}, continuing...`);
    }

    // Pre-check: verify Jitsi token has recordingEnabled=true
    try {
      const zoomLink = runSQL(`SELECT zoom_link FROM lessons WHERE id = ${lessonId}`);
      const roomName = zoomLink?.split('/').pop() || `mychess-lesson-${lessonId}`;
      const tokenResp = await context.request.get(`${API_BASE}/jitsi/token?roomName=${roomName}`);
      if (tokenResp.ok()) {
        const tokenData = await tokenResp.json();
        log('VIDEO', `Lesson ${lessonId}: recordingEnabled=${tokenData.recordingEnabled}, isModerator=${tokenData.isModerator}`);
        if (!tokenData.recordingEnabled) {
          log('VIDEO', `⚠ Lesson ${lessonId}: Recording NOT enabled in JWT (student not Premium or lesson not group?)`);
        }
      } else {
        log('VIDEO', `⚠ Lesson ${lessonId}: Jitsi token request failed (${tokenResp.status()})`);
      }
    } catch (e: any) {
      log('VIDEO', `Jitsi token pre-check error: ${e.message?.substring(0, 100)}`);
    }
  }

  // Navigate to lessons page with auto-join
  await page.goto(`/lessons?join=${lessonId}`, { waitUntil: 'domcontentloaded' });
  await dismissCookieBanner(page);

  // Wait for video overlay — try auto-join first, then manual button
  let overlayVisible = false;
  try {
    await page.waitForSelector('.video-call-overlay', { state: 'visible', timeout: 15_000 });
    overlayVisible = true;
  } catch {
    // Auto-join didn't work, try clicking the "Rejoindre" button
    const joinBtn = page.locator('button:has-text("Rejoindre"), button:has-text("Join")');
    if ((await joinBtn.count()) > 0) {
      await joinBtn.first().click();
      try {
        await page.waitForSelector('.video-call-overlay', { state: 'visible', timeout: 10_000 });
        overlayVisible = true;
      } catch {
        // Still no overlay
      }
    }
  }

  if (!overlayVisible) {
    await debugScreenshot(page, `video-no-overlay-${lessonId}`);
    throw new Error('Overlay video non visible');
  }

  // Wait for the Jitsi iframe to actually load inside the video-call-content container
  log('VIDEO', `Lesson ${lessonId}: overlay visible, waiting for Jitsi iframe...`);
  try {
    await page.waitForSelector('.video-call-content iframe', { state: 'attached', timeout: 30_000 });
    log('VIDEO', `Lesson ${lessonId}: Jitsi iframe loaded`);
  } catch {
    await debugScreenshot(page, `video-no-iframe-${lessonId}`);
    // Log captured console messages for diagnostics
    if (jitsiLogs.length > 0) {
      log('VIDEO', `Lesson ${lessonId} console logs: ${jitsiLogs.slice(0, 5).join(' | ')}`);
    }
    throw new Error('Jitsi iframe not loaded after 30s');
  }

  // Give Jitsi a moment to fully connect after iframe appears
  await sleep(3_000);

  // Log any Jitsi-related console messages
  if (jitsiLogs.length > 0) {
    log('VIDEO', `Lesson ${lessonId}: ${jitsiLogs.length} Jitsi logs captured`);
    for (const l of jitsiLogs.slice(0, 3)) {
      log('VIDEO', `  ${l.substring(0, 120)}`);
    }
  }
}

/** Recupere les cours a venir d'un user */
async function getUpcomingLessonIds(context: BrowserContext): Promise<number[]> {
  const response = await context.request.get(`${API_BASE}/lessons/upcoming`);
  if (!response.ok()) return [];
  const data = await response.json();
  const list = data.content || data;
  return list.map((l: any) => l.id);
}

// ============================================================================
// TEST PRINCIPAL
// ============================================================================

test('Stress test: 20 utilisateurs — 10 cours individuels paralleles avec enregistrement video', async ({ browser }) => {
  test.setTimeout(1_800_000); // 30 min (registration + login + booking + 5min wait + 3min call + 90s webhook wait)

  const baseURL = test.info().project.use.baseURL || 'https://mychess.fr';
  const results: PhaseResult[] = [];
  const allContexts: BrowserContext[] = [];
  const testStart = Date.now();

  console.log('\n' + '='.repeat(70));
  console.log('  STRESS TEST mychess.fr — 20 utilisateurs, 10 appels paralleles');
  console.log('  Base URL: ' + baseURL);
  console.log('  Date: ' + new Date().toISOString());
  console.log('  Config: BATCH_SIZE=' + BATCH_SIZE + ', CALL_DURATION=' + CALL_DURATION_MS / 1000 + 's');
  console.log('='.repeat(70));

  const { coaches, students } = generateUsers();
  const allUsers: TestUser[] = [...coaches, ...students]; // 20 total

  console.log(`  Utilisateurs: ${coaches.length} coachs + ${students.length} eleves = ${allUsers.length} (${coaches.length} appels paralleles)`);

  // ========================================================================
  // PHASE 1: Inscription des 20 utilisateurs via Playwright UI
  // ========================================================================
  console.log('\n[PHASE 1] Inscription de 20 utilisateurs via formulaire Playwright...');
  const p1Start = Date.now();
  const p1Fails: string[] = [];

  const regResults = await runInBatches(allUsers, BATCH_SIZE, user =>
    safeRun(user.email, 'REGISTER', async () => {
      const ctx = await browser.newContext({ baseURL });
      const pg = await ctx.newPage();
      try {
        await registerUser(pg, user);
      } finally {
        await pg.close();
        await ctx.close();
      }
      return true;
    })
  );

  regResults.forEach((r, i) => { if (r === null) p1Fails.push(allUsers[i].email); });
  results.push({
    name: 'Inscription (UI)',
    success: regResults.filter(r => r !== null).length,
    total: allUsers.length,
    failures: p1Fails,
    durationMs: Date.now() - p1Start,
  });

  await sleep(PHASE_DELAY_MS);

  // ========================================================================
  // PHASE 1b: Verification emails + wallet funding via DB
  // ========================================================================
  console.log('\n[PHASE 1b] Verification emails, wallets et Premium via DB...');
  verifyAllStressEmails();
  fundAllStudentWallets();
  grantAllStudentsPremium();

  for (const user of allUsers) {
    user.userId = getUserIdByEmail(user.email) ?? undefined;
  }

  await sleep(PHASE_DELAY_MS);

  // ========================================================================
  // PHASE 2: Connexion des 20 utilisateurs via Playwright UI
  // ========================================================================
  console.log('\n[PHASE 2] Connexion de 20 utilisateurs via formulaire Playwright...');
  const p2Start = Date.now();
  const p2Fails: string[] = [];

  const loginResults = await runInBatches(allUsers, LOGIN_BATCH_SIZE, user =>
    safeRun(user.email, 'LOGIN', async () => {
      const context = await browser.newContext({
        baseURL,
        permissions: ['camera', 'microphone'],
      });
      allContexts.push(context);
      const page = await context.newPage();

      try {
        await loginUser(page, user);
        user.context = context;
        user.page = page;
      } catch (e) {
        await page.close().catch(() => {});
        await context.close().catch(() => {});
        throw e;
      }
      return true;
    }),
    LOGIN_INTER_BATCH_DELAY_MS // Delay between batches to avoid browser overload
  );

  loginResults.forEach((r, i) => { if (r === null) p2Fails.push(allUsers[i].email); });
  results.push({
    name: 'Connexion (UI)',
    success: loginResults.filter(r => r !== null).length,
    total: allUsers.length,
    failures: p2Fails,
    durationMs: Date.now() - p2Start,
  });

  await sleep(PHASE_DELAY_MS);

  // ========================================================================
  // PHASE 3: Coachs creent des disponibilites via Playwright UI
  // ========================================================================
  console.log(`\n[PHASE 3] Creation des disponibilites pour ${coaches.length} coachs via Playwright...`);
  const p3Start = Date.now();
  const p3Fails: string[] = [];

  // Run in batches to avoid overloading the server with 10 simultaneous availability creations
  const availResults = await runInBatches(
    coaches.map((coach, i) => ({ coach, i })),
    BATCH_SIZE,
    ({ coach, i }) =>
      safeRun(coach.email, 'DISPO', async () => {
        if (!coach.page) throw new Error('Pas de page (login echoue)');
        await createAvailability(coach.page, 'INDIVIDUAL', i);
        return true;
      }),
    2_000
  );

  availResults.forEach((r, i) => { if (r === null) p3Fails.push(coaches[i].email); });
  results.push({
    name: 'Disponibilites (UI)',
    success: availResults.filter(r => r !== null).length,
    total: coaches.length,
    failures: p3Fails,
    durationMs: Date.now() - p3Start,
  });

  await sleep(PHASE_DELAY_MS);

  // ========================================================================
  // PHASE 3b: Resolution des IDs des coachs via DB
  // ========================================================================
  console.log('\n[PHASE 3b] Resolution des IDs des coachs via DB...');
  for (const coach of coaches) {
    const userId = getUserIdByEmail(coach.email);
    if (userId) {
      coach.userId = userId;
      coach.teacherId = userId;
      log('RESOLVE', `${coach.firstName} -> teacherId=${userId}`);
    } else {
      log('RESOLVE', `\u2717 ${coach.email}: non trouve en DB`);
    }
  }

  // Pair students with coaches 1:1 (only those with teacherId AND page)
  students.forEach((s, i) => {
    if (coaches[i]?.teacherId && coaches[i]?.page) {
      s.teacherId = coaches[i].teacherId;
    }
  });

  // ========================================================================
  // PHASE 4: Reservation de 10 cours individuels via Playwright UI
  // ========================================================================
  console.log(`\n[PHASE 4] Reservation de ${students.length} cours individuels via Playwright...`);
  const p4Start = Date.now();
  const p4Fails: string[] = [];

  const bookResults = await runInBatches(
    students.map((student, i) => ({ student, i })),
    BATCH_SIZE,
    ({ student, i }) =>
      safeRun(student.email, 'BOOK_IND', async () => {
        if (!student.page || !student.teacherId) throw new Error('Pas de page ou coach non appaire');
        await bookIndividualLesson(student.page, student.teacherId);

        const lessonIds = await getUpcomingLessonIds(student.context!);
        if (lessonIds.length > 0) {
          student.lessonId = lessonIds[0];
          coaches[i].lessonId = lessonIds[0];
        }
        return student.lessonId || true;
      }),
    3_000
  );

  bookResults.forEach((r, i) => { if (r === null) p4Fails.push(students[i].email); });
  results.push({
    name: 'Reservation individuel (UI)',
    success: bookResults.filter(r => r !== null).length,
    total: students.length,
    failures: p4Fails,
    durationMs: Date.now() - p4Start,
  });

  await sleep(PHASE_DELAY_MS);

  // ========================================================================
  // PHASE 5: Coachs confirment les cours
  // ========================================================================
  console.log(`\n[PHASE 5] Confirmation des ${coaches.length} cours par les coachs...`);
  const p5Start = Date.now();
  const p5Fails: string[] = [];

  const confirmResults = await Promise.all(
    coaches.map(coach =>
      safeRun(coach.email, 'CONFIRM', async () => {
        if (!coach.context || !coach.lessonId) throw new Error('Pas de context ou lessonId');
        await confirmLesson(coach.context, coach.lessonId);
        return true;
      })
    )
  );

  confirmResults.forEach((r, i) => { if (r === null) p5Fails.push(coaches[i].email); });
  results.push({
    name: 'Confirmation cours',
    success: confirmResults.filter(r => r !== null).length,
    total: coaches.length,
    failures: p5Fails,
    durationMs: Date.now() - p5Start,
  });

  await sleep(PHASE_DELAY_MS);

  // ========================================================================
  // PHASE 5b: Rendre les cours rejoignables (scheduled_at = NOW()+5min)
  // ========================================================================
  console.log('\n[PHASE 5b] Mise a jour DB: scheduled_at -> NOW()+5min pour que les cours demarrent dans 5 minutes...');
  makeAllStressLessonsJoinable();

  // Also resolve lessonIds for students via DB (in case API didn't return them)
  for (const student of students) {
    if (!student.lessonId && student.userId) {
      const lid = runSQL(`
        SELECT l.id FROM lessons l
        LEFT JOIN lesson_participants lp ON lp.lesson_id = l.id
        WHERE (l.student_id = ${student.userId} OR lp.student_id = ${student.userId})
        AND l.status = 'CONFIRMED'
        ORDER BY l.id DESC LIMIT 1
      `);
      if (lid) {
        student.lessonId = parseInt(lid, 10);
        log('RESOLVE', `${student.firstName} -> lessonId=${student.lessonId}`);
      }
    }
  }

  // Wait ~3 minutes so the lessons are about to start when users join
  // (scheduled_at = NOW()+5min, we already spent ~2min on phases above)
  const WAIT_BEFORE_JOIN_MS = 3 * 60_000; // 3 minutes
  console.log(`\n[PHASE 5c] Attente de ${WAIT_BEFORE_JOIN_MS / 60_000} minutes avant de rejoindre les appels...`);
  console.log('  (Les cours demarrent dans ~2 minutes)');
  const waitStart = Date.now();
  while (Date.now() - waitStart < WAIT_BEFORE_JOIN_MS) {
    const remaining = Math.ceil((WAIT_BEFORE_JOIN_MS - (Date.now() - waitStart)) / 1000);
    if (remaining % 30 === 0) {
      log('WAIT', `${remaining}s restantes avant de rejoindre les appels...`);
    }
    await sleep(1_000);
  }
  console.log('  Attente terminee — les cours commencent maintenant!');

  // ========================================================================
  // PHASE 6: 20 utilisateurs rejoignent les 10 appels video
  // ========================================================================
  console.log('\n[PHASE 6] 20 utilisateurs rejoignent les 10 appels video via Playwright...');
  const p6Start = Date.now();
  const p6Fails: string[] = [];

  const videoParticipants = allUsers.filter(u => u.page && u.context && u.lessonId);
  const coachParticipants = videoParticipants.filter(u => u.role === 'TEACHER');
  const studentParticipants = videoParticipants.filter(u => u.role === 'STUDENT');

  log('VIDEO', `Participants eligibles: ${coachParticipants.length} coachs + ${studentParticipants.length} eleves`);

  // Coaches join first (they must mark teacher-joined to unlock the button for students)
  const coachVideoResults = await Promise.all(
    coachParticipants.map(user =>
      safeRun(user.email, 'VIDEO_COACH', async () => {
        await joinVideoCall(user.page!, user.context!, user.lessonId!, true);
        return true;
      })
    )
  );

  await sleep(5_000); // Let coaches connect before students

  // Students join
  const studentVideoResults = await Promise.all(
    studentParticipants.map(user =>
      safeRun(user.email, 'VIDEO_STUDENT', async () => {
        await joinVideoCall(user.page!, user.context!, user.lessonId!, false);
        return true;
      })
    )
  );

  const allVideoResults = [...coachVideoResults, ...studentVideoResults];
  const allVideoParticipants = [...coachParticipants, ...studentParticipants];
  allVideoResults.forEach((r, i) => {
    if (r === null) p6Fails.push(allVideoParticipants[i]?.email || 'unknown');
  });

  results.push({
    name: 'Appels video (UI)',
    success: allVideoResults.filter(r => r !== null).length,
    total: videoParticipants.length,
    failures: p6Fails,
    durationMs: Date.now() - p6Start,
  });

  // ========================================================================
  // PHASE 7: Maintien en appel pour enregistrement video
  // ========================================================================
  const usersInCall = allVideoResults.filter(r => r !== null).length;
  console.log(`\n[PHASE 7] ${usersInCall} utilisateurs en appel video — maintien ${CALL_DURATION_MS / 1000}s pour enregistrement...`);
  console.log('  (L\'enregistrement Jibri demarre automatiquement pour les cours Premium)');

  // Check recording status after 10s (recording auto-starts 3s after teacher joins)
  await sleep(10_000);
  for (const user of coachParticipants) {
    if (user.page) {
      try {
        const recIndicator = user.page.locator('.recording-indicator, [class*="recording"]');
        const isRecording = (await recIndicator.count()) > 0;
        log('VIDEO', `Lesson ${user.lessonId}: recording indicator visible = ${isRecording}`);
      } catch { /* ignore */ }
    }
  }

  await sleep(CALL_DURATION_MS - 10_000);
  console.log('  Fin du maintien en appel.');

  // ========================================================================
  // PHASE 7b: Marquer les cours comme COMPLETED et attendre le webhook Jibri
  // ========================================================================
  console.log('\n[PHASE 7b] Passage des cours en COMPLETED + attente webhook Jibri...');
  completeAllStressLessons();

  // Jibri envoie le webhook apres avoir finalise l'enregistrement (finalize.sh)
  // Cela prend generalement 30-60s apres la deconnexion de tous les participants
  const WEBHOOK_WAIT_MS = 90_000; // 90 secondes
  console.log(`  Attente ${WEBHOOK_WAIT_MS / 1000}s pour la finalisation Jibri et le webhook...`);
  const webhookWaitStart = Date.now();
  while (Date.now() - webhookWaitStart < WEBHOOK_WAIT_MS) {
    const remaining = Math.ceil((WEBHOOK_WAIT_MS - (Date.now() - webhookWaitStart)) / 1000);
    if (remaining % 30 === 0) {
      // Check intermediate state
      const intermediate = checkRecordingsInDB();
      log('WAIT', `${remaining}s restantes — recording_url: ${intermediate.withRecording}, segments: ${intermediate.withSegments}`);
      if (intermediate.withRecording > 0 || intermediate.withSegments > 0) {
        log('WAIT', 'Enregistrements detectes! Fin de l\'attente.');
        break;
      }
    }
    await sleep(1_000);
  }

  // ========================================================================
  // PHASE 8: Verification des enregistrements video en DB
  // ========================================================================
  console.log('\n[PHASE 8] Verification des enregistrements video en base de donnees...');
  const p8Start = Date.now();

  const recordingCheck = checkRecordingsInDB();
  console.log(`\n  Enregistrements trouves:`);
  console.log(`    Cours (confirmes+completes): ${recordingCheck.total}`);
  console.log(`    Avec recording_url: ${recordingCheck.withRecording}/${recordingCheck.total}`);
  console.log(`    Avec recording_segments: ${recordingCheck.withSegments}/${recordingCheck.total}`);
  for (const detail of recordingCheck.details) {
    console.log(detail);
  }

  // Diagnostic: check Jibri recordings directory for new files
  const jibriDiag = runSQL(`SELECT COUNT(*) FROM lessons WHERE recording_url IS NOT NULL`);
  const recordingDirCheck = (() => {
    try {
      const result = execSync(
        `docker exec mychess-backend sh -c "ls -lt /var/jibri/recordings/ 2>/dev/null | head -5"`,
        { encoding: 'utf-8', timeout: 5_000 }
      ).trim();
      return result;
    } catch { return 'N/A'; }
  })();
  console.log(`\n  Diagnostic Jibri:`);
  console.log(`    Recordings totales en DB: ${jibriDiag}`);
  console.log(`    Dossier /var/jibri/recordings/:`);
  for (const line of recordingDirCheck.split('\n')) {
    console.log(`      ${line}`);
  }

  results.push({
    name: 'Enregistrements video (DB)',
    success: recordingCheck.withRecording + recordingCheck.withSegments,
    total: recordingCheck.total, // 1 check per lesson
    failures: recordingCheck.withRecording === 0 && recordingCheck.total > 0
      ? ['Aucun recording — verifier que Jibri est actif sur meet.mychess.fr']
      : [],
    durationMs: Date.now() - p8Start,
  });

  // ========================================================================
  // PHASE 9: Nettoyage
  // ========================================================================
  console.log('\n[PHASE 9] Fermeture des contextes...');
  for (const ctx of allContexts) {
    try { await ctx.close(); } catch { /* ignore */ }
  }

  // ========================================================================
  // RESUME FINAL
  // ========================================================================
  const totalDuration = Date.now() - testStart;

  console.log('\n' + '='.repeat(70));
  console.log('  RESULTATS DU STRESS TEST — 10 APPELS PARALLELES + ENREGISTREMENT');
  console.log('='.repeat(70));
  console.log('');

  for (const r of results) {
    const icon = r.success === r.total ? '\u2713' : (r.success > 0 ? '~' : '\u2717');
    const pct = r.total > 0 ? ((r.success / r.total) * 100).toFixed(0) : 'N/A';
    console.log(`  ${icon} ${r.name.padEnd(30)} ${r.success}/${r.total} (${pct}%) — ${(r.durationMs / 1000).toFixed(1)}s`);
    for (const f of r.failures.slice(0, 3)) {
      console.log(`      \u2717 ${f}`);
    }
    if (r.failures.length > 3) {
      console.log(`      ... et ${r.failures.length - 3} autre(s)`);
    }
  }

  console.log('');
  console.log(`  Duree totale: ${(totalDuration / 1000).toFixed(1)}s (${(totalDuration / 60_000).toFixed(1)}min)`);
  console.log(`  Contextes ouverts: ${allContexts.length}`);
  console.log('');

  // Video recording verdict
  if (recordingCheck.withRecording > 0 || recordingCheck.withSegments > 0) {
    console.log('  \u2713 ENREGISTREMENT VIDEO: Des enregistrements ont ete detectes en DB');
  } else if (recordingCheck.total > 0) {
    console.log('  \u26a0 ENREGISTREMENT VIDEO: Aucun enregistrement detecte');
    console.log('    Note: Jibri doit etre actif et configure pour que les enregistrements fonctionnent.');
    console.log('    Les appels video ont bien eu lieu, mais le recording necessite Jibri.');
  } else {
    console.log('  \u2717 ENREGISTREMENT VIDEO: Aucun cours trouve (ni confirme ni complete) — impossible de verifier');
  }

  console.log('='.repeat(70) + '\n');
});
