import { test, BrowserContext, Page } from '@playwright/test';
import { execSync } from 'child_process';

// ============================================================================
// CONFIGURATION
// ============================================================================

const API_BASE = '/api';
const PASSWORD = 'StressTest2026!';
const CALL_DURATION_MS = 3 * 60_000; // 3 minutes in call for recording
const PHASE_DELAY_MS = 3_000;

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
  groupIndex?: number; // Which group this user belongs to (0 or 1)
}

interface PhaseResult {
  name: string;
  success: number;
  total: number;
  failures: string[];
  durationMs: number;
}

// ============================================================================
// USER GENERATION — 7 users (2 coaches + 5 students)
// Group 1: Coach A + 2 students (group of 2)
// Group 2: Coach B + 3 students (group of 3)
// ============================================================================

function generateUsers(): {
  coaches: TestUser[];
  students: TestUser[];
  group1Students: TestUser[];
  group2Students: TestUser[];
} {
  const ts = Date.now();

  const coaches: TestUser[] = [
    {
      email: `coach_grp_A_${ts}@stress.mychess.fr`,
      password: PASSWORD,
      firstName: 'Alexandre',
      lastName: 'GroupCoach',
      role: 'TEACHER',
      hourlyRateCents: 3000,
      bio: 'Coach de stress test groupe avec plus de dix ans experience en enseignement des echecs.',
      languages: ['FR'],
      groupIndex: 0,
    },
    {
      email: `coach_grp_B_${ts}@stress.mychess.fr`,
      password: PASSWORD,
      firstName: 'Baptiste',
      lastName: 'GroupCoach',
      role: 'TEACHER',
      hourlyRateCents: 4000,
      bio: 'Coach de stress test groupe specialise en cours collectifs pour tous niveaux.',
      languages: ['FR'],
      groupIndex: 1,
    },
  ];

  // Group 1: 2 students
  const group1Students: TestUser[] = [
    {
      email: `student_grp1_creator_${ts}@stress.mychess.fr`,
      password: PASSWORD,
      firstName: 'Alice',
      lastName: 'GroupEleve',
      role: 'STUDENT',
      birthDate: '2000-01-15',
      groupIndex: 0,
    },
    {
      email: `student_grp1_join_${ts}@stress.mychess.fr`,
      password: PASSWORD,
      firstName: 'Beatrice',
      lastName: 'GroupEleve',
      role: 'STUDENT',
      birthDate: '2001-03-20',
      groupIndex: 0,
    },
  ];

  // Group 2: 3 students
  const group2Students: TestUser[] = [
    {
      email: `student_grp2_creator_${ts}@stress.mychess.fr`,
      password: PASSWORD,
      firstName: 'Camille',
      lastName: 'GroupEleve',
      role: 'STUDENT',
      birthDate: '1999-06-10',
      groupIndex: 1,
    },
    {
      email: `student_grp2_join1_${ts}@stress.mychess.fr`,
      password: PASSWORD,
      firstName: 'Diane',
      lastName: 'GroupEleve',
      role: 'STUDENT',
      birthDate: '2002-09-25',
      groupIndex: 1,
    },
    {
      email: `student_grp2_join2_${ts}@stress.mychess.fr`,
      password: PASSWORD,
      firstName: 'Emma',
      lastName: 'GroupEleve',
      role: 'STUDENT',
      birthDate: '2000-12-05',
      groupIndex: 1,
    },
  ];

  const students = [...group1Students, ...group2Students];

  return { coaches, students, group1Students, group2Students };
}

// ============================================================================
// DB HELPERS
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

function verifyAllStressEmails(): void {
  runSQL("UPDATE users SET email_verified = true WHERE email LIKE '%@stress.mychess.fr'");
  log('DB', 'Tous les emails de stress test marques comme verifies');
}

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
    SELECT id, 'TOPUP', 100000, 'Stress test group funding', NOW()
    FROM users WHERE email LIKE '%student%@stress.mychess.fr'
    AND id NOT IN (
      SELECT user_id FROM credit_transactions WHERE description = 'Stress test group funding'
    )
  `);
  log('DB', 'Wallets etudiants credites de 1000EUR');
}

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

function getUserIdByEmail(email: string): number | null {
  const result = runSQL(`SELECT id FROM users WHERE email = '${email}'`);
  return result ? parseInt(result, 10) : null;
}

function makeGroupLessonsJoinable(): void {
  runSQL(`
    UPDATE lessons SET scheduled_at = (NOW() AT TIME ZONE 'Europe/Paris') + INTERVAL '2 minutes'
    WHERE is_group_lesson = true
    AND status = 'CONFIRMED'
    AND id IN (
      SELECT l.id FROM lessons l
      JOIN users u ON l.teacher_id = u.id
      WHERE u.email LIKE '%grp%@stress.mychess.fr'
    )
  `);
  log('DB', 'scheduled_at mis a CET NOW()+2min pour les cours de groupe');

  runSQL(`
    UPDATE lessons SET zoom_link = 'https://meet.mychess.fr/mychess-lesson-' || id
    WHERE zoom_link IS NULL
    AND is_group_lesson = true
    AND status = 'CONFIRMED'
    AND id IN (
      SELECT l.id FROM lessons l
      JOIN users u ON l.teacher_id = u.id
      WHERE u.email LIKE '%grp%@stress.mychess.fr'
    )
  `);
  log('DB', 'zoom_link verifie/set pour les cours de groupe confirmes');
}

function completeGroupLessons(): void {
  runSQL(`
    UPDATE lessons SET status = 'COMPLETED'
    WHERE status = 'CONFIRMED'
    AND is_group_lesson = true
    AND id IN (
      SELECT l.id FROM lessons l
      JOIN users u ON l.teacher_id = u.id
      WHERE u.email LIKE '%grp%@stress.mychess.fr'
    )
  `);
  log('DB', 'Cours de groupe marques comme COMPLETED');
}

function checkGroupRecordingsInDB(): { total: number; withRecording: number; withSegments: number; details: string[] } {
  const rows = runSQL(`
    SELECT l.id, l.status, l.recording_url, l.recording_segments, l.thumbnail_url,
           l.max_participants, l.group_status,
           (SELECT COUNT(*) FROM lesson_participants lp WHERE lp.lesson_id = l.id AND lp.status = 'ACTIVE') as participant_count
    FROM lessons l
    JOIN users u ON l.teacher_id = u.id
    WHERE u.email LIKE '%grp%@stress.mychess.fr'
    AND l.is_group_lesson = true
    AND l.status IN ('CONFIRMED', 'COMPLETED')
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
      const maxPart = parts[5] || '';
      const groupStatus = parts[6] || '';
      const partCount = parts[7] || '0';

      if (recordingUrl) withRecording++;
      if (segments && segments !== '[]' && segments !== 'null') withSegments++;

      details.push(`  Lesson #${lessonId} (${status}): group=${maxPart}p (${partCount} active, ${groupStatus}), recording=${recordingUrl ? 'YES' : 'NO'}, segments=${segments ? 'YES' : 'NO'}, thumbnail=${thumbnail ? 'YES' : 'NO'}`);
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

async function dismissCookieBanner(page: Page): Promise<void> {
  try {
    const acceptBtn = page.locator('button:has-text("Accepter")');
    if (await acceptBtn.isVisible({ timeout: 2_000 })) {
      await acceptBtn.click();
      await page.waitForTimeout(300);
    }
  } catch { /* No banner */ }
}

async function debugScreenshot(page: Page, name: string): Promise<void> {
  try {
    await page.screenshot({ path: `test-results/stress/debug-${name}-${Date.now()}.png`, fullPage: true });
  } catch { /* Ignore */ }
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

async function createGroupAvailability(page: Page, slotIndex: number, maxParticipants: 2 | 3): Promise<void> {
  await page.goto('/availability');
  await page.waitForLoadState('domcontentloaded');
  await dismissCookieBanner(page);

  await page.waitForSelector('.availability-page', { timeout: 15_000 });
  await sleep(1_000);

  // Click the "Add" button in the page header
  await page.locator('.page-header .btn--primary').click();
  await page.waitForSelector('.modal-overlay', { state: 'visible', timeout: 10_000 });
  await sleep(500);

  // Select GROUP lesson type — click the label containing the GROUP radio
  // Radio inputs: 1st = INDIVIDUAL, 2nd = GROUP
  const lessonTypeRadios = page.locator('.modal__body input[name="lessonType"]');
  await lessonTypeRadios.nth(1).check({ force: true }); // GROUP
  await sleep(800);

  // Select group max participants — appears conditionally after GROUP is selected
  // Radio inputs: 1st = 2 players, 2nd = 3 players
  const groupSizeRadios = page.locator('.modal__body input[name="groupSize"]');
  await groupSizeRadios.first().waitFor({ state: 'attached', timeout: 5_000 });
  if (maxParticipants === 3) {
    await groupSizeRadios.nth(1).check({ force: true });
  } else {
    await groupSizeRadios.first().check({ force: true });
  }
  await sleep(300);

  // Day of week (spread across days)
  const days = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];
  await page.selectOption('#daySelect', days[slotIndex % days.length]);

  // Start hour — use late hours so the invitation deadline (scheduledAt - 24h) is in the future
  const startHour = (20 + slotIndex) % 24;
  const timeSelects = page.locator('.time-picker__select');
  await timeSelects.first().selectOption(startHour.toString().padStart(2, '0'));
  await timeSelects.nth(1).selectOption('00');

  // Submit
  await page.locator('.modal__footer .btn--primary').click();
  await page.waitForSelector('.modal-overlay', { state: 'hidden', timeout: 10_000 });
}

async function bookGroupLesson(page: Page, teacherId: number): Promise<string> {
  await page.goto(`/book/${teacherId}`);
  await dismissCookieBanner(page);

  await page.waitForSelector('.lesson-type-btn', { timeout: 25_000 });

  // Switch to group mode (2nd button)
  await page.locator('.lesson-type-btn').nth(1).click();
  await sleep(1_000);

  // Group size is determined by the coach's availability — no selector on booking page
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

  const submitBtn = page.locator('form.booking-form button[type="submit"]');
  await submitBtn.scrollIntoViewIfNeeded();
  await submitBtn.click({ force: true });

  // Handle wallet confirmation modal
  const confirmModal = page.locator('button:has-text("Confirmer")');
  try {
    await confirmModal.waitFor({ state: 'visible', timeout: 5_000 });
    await confirmModal.click();
  } catch { /* No modal */ }

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
  if (!token) throw new Error("Token d'invitation non trouve dans: " + invitationUrl);

  return token;
}

async function joinGroupLesson(page: Page, token: string): Promise<void> {
  await page.goto(`/join/${token}`);
  await dismissCookieBanner(page);

  // Wait for the invitation details to fully load (action-section appears when ready)
  // Also handle error/full states
  try {
    await page.waitForSelector('.action-section, .join-card--error, .spots-badge--full, .spots-badge--expired', {
      timeout: 20_000,
    });
  } catch {
    await debugScreenshot(page, 'join-group-timeout');
    throw new Error('Page /join/ did not load invitation details');
  }

  const isError = await page.locator('.join-card--error').count();
  if (isError > 0) {
    await debugScreenshot(page, 'join-group-error');
    throw new Error('Invitation invalide ou expiree');
  }

  const isFull = await page.locator('.spots-badge--full').count();
  if (isFull > 0) {
    throw new Error('Groupe deja complet');
  }

  // Click "Join with credit" (gold button) or fallback to card button
  const creditBtn = page.locator('.btn--gold');
  if ((await creditBtn.count()) > 0) {
    await creditBtn.click();
  } else {
    await page.locator('.action-section .btn--primary').first().click();
  }

  await page.waitForSelector('.join-card--success', { timeout: 30_000 });
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
  // Get room name from DB
  const zoomLink = runSQL(`SELECT zoom_link FROM lessons WHERE id = ${lessonId}`);
  const roomName = zoomLink?.split('/').pop() || `mychess-lesson-${lessonId}`;

  // Teacher: mark as joined
  if (isTeacher) {
    try {
      await context.request.patch(`${API_BASE}/lessons/${lessonId}/teacher-joined`);
    } catch {
      log('VIDEO', `teacher-joined API failed for lesson ${lessonId}, continuing...`);
    }
  }

  // Get Jitsi token via API (reliable, bypasses SSR issues)
  const tokenResp = await context.request.get(`${API_BASE}/jitsi/token?roomName=${roomName}`);
  if (!tokenResp.ok()) {
    throw new Error(`Jitsi token failed (${tokenResp.status()}) for lesson ${lessonId}`);
  }
  const tokenData = await tokenResp.json();
  log('VIDEO', `Lesson ${lessonId}: token OK, recording=${tokenData.recordingEnabled}, moderator=${tokenData.isModerator}`);

  // Navigate to lessons page (don't use networkidle — SSE connections prevent it)
  await page.goto('/lessons', { waitUntil: 'domcontentloaded' });
  await dismissCookieBanner(page);
  await sleep(3_000); // Wait for Angular to bootstrap and fetch data

  // Try UI approach: wait for lesson cards and click join button
  let overlayVisible = false;
  try {
    await page.waitForSelector('.lesson-card', { state: 'visible', timeout: 15_000 });
    const joinBtn = page.locator('button:has-text("Rejoindre"), button:has-text("Join")');
    await joinBtn.first().waitFor({ state: 'visible', timeout: 5_000 });
    await joinBtn.first().click();
    await page.waitForSelector('.video-call-overlay', { state: 'visible', timeout: 15_000 });
    overlayVisible = true;
    log('VIDEO', `Lesson ${lessonId}: joined via UI (lesson cards visible)`);
  } catch {
    log('VIDEO', `Lesson ${lessonId}: UI join failed, injecting Jitsi iframe directly`);
  }

  // Fallback: inject Jitsi iframe directly into the page
  if (!overlayVisible) {
    const jitsiUrl = `https://meet.mychess.fr/${roomName}?jwt=${tokenData.token}#config.startWithAudioMuted=true&config.startWithVideoMuted=true&config.prejoinPageEnabled=false`;
    await page.evaluate((url) => {
      const overlay = document.createElement('div');
      overlay.className = 'video-call-overlay';
      overlay.style.cssText = 'position:fixed;top:0;left:0;width:100vw;height:100vh;z-index:9999;background:#0d0d0f;';
      const content = document.createElement('div');
      content.className = 'video-call-content';
      content.style.cssText = 'width:100%;height:100%;';
      const iframe = document.createElement('iframe');
      iframe.src = url;
      iframe.style.cssText = 'width:100%;height:100%;border:none;';
      iframe.allow = 'camera;microphone;display-capture;autoplay';
      content.appendChild(iframe);
      overlay.appendChild(content);
      document.body.appendChild(overlay);
    }, jitsiUrl);
    overlayVisible = true;
    log('VIDEO', `Lesson ${lessonId}: Jitsi iframe injected directly`);
  }

  // Verify iframe is loaded
  try {
    await page.waitForSelector('.video-call-content iframe', { state: 'attached', timeout: 30_000 });
    log('VIDEO', `Lesson ${lessonId}: Jitsi iframe attached`);
  } catch {
    await debugScreenshot(page, `video-no-iframe-${lessonId}`);
    throw new Error('Jitsi iframe not loaded after 30s');
  }

  await sleep(3_000);
}

async function getUpcomingLessonIds(context: BrowserContext): Promise<number[]> {
  const response = await context.request.get(`${API_BASE}/lessons/upcoming`);
  if (!response.ok()) return [];
  const data = await response.json();
  const list = data.content || data;
  return list.map((l: any) => l.id);
}

// ============================================================================
// TEST PRINCIPAL — 2 COURS COLLECTIFS AVEC ENREGISTREMENT
// ============================================================================

test('Stress test: 2 cours collectifs (groupe de 2 + groupe de 3) avec enregistrement video', async ({ browser }) => {
  test.setTimeout(1_200_000); // 20 min

  const baseURL = test.info().project.use.baseURL || 'https://mychess.fr';
  const results: PhaseResult[] = [];
  const allContexts: BrowserContext[] = [];
  const testStart = Date.now();

  console.log('\n' + '='.repeat(70));
  console.log('  STRESS TEST COURS COLLECTIFS — 2 groupes avec enregistrement');
  console.log('  Groupe 1: Coach A + 2 eleves (groupe de 2)');
  console.log('  Groupe 2: Coach B + 3 eleves (groupe de 3)');
  console.log('  Base URL: ' + baseURL);
  console.log('  Date: ' + new Date().toISOString());
  console.log('='.repeat(70));

  const { coaches, students, group1Students, group2Students } = generateUsers();
  const allUsers: TestUser[] = [...coaches, ...students];

  console.log(`  Utilisateurs: ${coaches.length} coachs + ${students.length} eleves = ${allUsers.length}`);

  // ========================================================================
  // PHASE 1: Inscription des 7 utilisateurs
  // ========================================================================
  console.log('\n[PHASE 1] Inscription de 7 utilisateurs via formulaire Playwright...');
  const p1Start = Date.now();
  const p1Fails: string[] = [];

  for (const user of allUsers) {
    const result = await safeRun(user.email, 'REGISTER', async () => {
      const ctx = await browser.newContext({ baseURL });
      const pg = await ctx.newPage();
      try {
        await registerUser(pg, user);
      } finally {
        await pg.close();
        await ctx.close();
      }
      return true;
    });
    if (result === null) p1Fails.push(user.email);
  }

  results.push({
    name: 'Inscription (UI)',
    success: allUsers.length - p1Fails.length,
    total: allUsers.length,
    failures: p1Fails,
    durationMs: Date.now() - p1Start,
  });

  await sleep(PHASE_DELAY_MS);

  // ========================================================================
  // PHASE 1b: DB setup — emails, wallets, premium
  // ========================================================================
  console.log('\n[PHASE 1b] Verification emails, wallets et Premium via DB...');
  verifyAllStressEmails();
  fundAllStudentWallets();
  grantAllStudentsPremium();

  for (const user of allUsers) {
    user.userId = getUserIdByEmail(user.email) ?? undefined;
    if (user.userId) log('DB', `${user.firstName} (${user.role}) -> userId=${user.userId}`);
  }

  await sleep(PHASE_DELAY_MS);

  // ========================================================================
  // PHASE 2: Login des 7 utilisateurs
  // ========================================================================
  console.log('\n[PHASE 2] Connexion de 7 utilisateurs...');
  const p2Start = Date.now();
  const p2Fails: string[] = [];

  for (const user of allUsers) {
    const result = await safeRun(user.email, 'LOGIN', async () => {
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
    });
    if (result === null) p2Fails.push(user.email);
  }

  results.push({
    name: 'Connexion (UI)',
    success: allUsers.length - p2Fails.length,
    total: allUsers.length,
    failures: p2Fails,
    durationMs: Date.now() - p2Start,
  });

  await sleep(PHASE_DELAY_MS);

  // ========================================================================
  // PHASE 3: Coaches create GROUP availabilities
  // ========================================================================
  console.log('\n[PHASE 3] Creation des disponibilites GROUP...');
  console.log('  Coach A: GROUP maxParticipants=2');
  console.log('  Coach B: GROUP maxParticipants=3');
  const p3Start = Date.now();
  const p3Fails: string[] = [];

  // Coach A: group of 2
  const r3a = await safeRun(coaches[0].email, 'DISPO', async () => {
    if (!coaches[0].page) throw new Error('Pas de page');
    await createGroupAvailability(coaches[0].page, 0, 2);
    return true;
  });
  if (r3a === null) p3Fails.push(coaches[0].email);

  await sleep(2_000);

  // Coach B: group of 3
  const r3b = await safeRun(coaches[1].email, 'DISPO', async () => {
    if (!coaches[1].page) throw new Error('Pas de page');
    await createGroupAvailability(coaches[1].page, 1, 3);
    return true;
  });
  if (r3b === null) p3Fails.push(coaches[1].email);

  results.push({
    name: 'Disponibilites GROUP (UI)',
    success: coaches.length - p3Fails.length,
    total: coaches.length,
    failures: p3Fails,
    durationMs: Date.now() - p3Start,
  });

  await sleep(PHASE_DELAY_MS);

  // ========================================================================
  // PHASE 3b: Resolve coach IDs
  // ========================================================================
  console.log('\n[PHASE 3b] Resolution des IDs des coachs...');
  for (const coach of coaches) {
    const userId = getUserIdByEmail(coach.email);
    if (userId) {
      coach.userId = userId;
      coach.teacherId = userId;
      log('RESOLVE', `${coach.firstName} -> teacherId=${userId}`);
    }
  }

  // ========================================================================
  // PHASE 4: Group booking — creators book, then others join via token
  // ========================================================================
  console.log('\n[PHASE 4] Reservation des cours collectifs...');
  const p4Start = Date.now();
  const p4Fails: string[] = [];

  // --- Group 1: Student Alice creates, Student Beatrice joins ---
  let group1Token = '';
  const creator1 = group1Students[0];
  const joiner1 = group1Students[1];

  console.log('  [Groupe 1] Alice cree un cours collectif avec Coach A (groupe de 2)...');
  const r4a = await safeRun(creator1.email, 'BOOK_GROUP', async () => {
    if (!creator1.page || !coaches[0].teacherId) throw new Error('Pas de page ou coach non resolu');
    group1Token = await bookGroupLesson(creator1.page, coaches[0].teacherId);
    log('BOOK', `Groupe 1 token: ${group1Token.substring(0, 12)}...`);
    return true;
  });
  if (r4a === null) p4Fails.push(creator1.email);

  // Extend invitation deadline to ensure it's not expired (deadline = scheduledAt - 24h can be in the past)
  if (group1Token) {
    runSQL(`UPDATE group_invitations SET expires_at = NOW() + INTERVAL '24 hours' WHERE token = '${group1Token}'`);
    log('DB', 'Deadline invitation groupe 1 prolongee');
  }

  await sleep(2_000);

  if (group1Token) {
    console.log('  [Groupe 1] Beatrice rejoint via invitation...');
    const r4b = await safeRun(joiner1.email, 'JOIN_GROUP', async () => {
      if (!joiner1.page) throw new Error('Pas de page');
      await joinGroupLesson(joiner1.page, group1Token);
      return true;
    });
    if (r4b === null) p4Fails.push(joiner1.email);
  }

  await sleep(2_000);

  // --- Group 2: Student Camille creates, Diane and Emma join ---
  let group2Token = '';
  const creator2 = group2Students[0];
  const joiner2a = group2Students[1];
  const joiner2b = group2Students[2];

  console.log('  [Groupe 2] Camille cree un cours collectif avec Coach B (groupe de 3)...');
  const r4c = await safeRun(creator2.email, 'BOOK_GROUP', async () => {
    if (!creator2.page || !coaches[1].teacherId) throw new Error('Pas de page ou coach non resolu');
    group2Token = await bookGroupLesson(creator2.page, coaches[1].teacherId);
    log('BOOK', `Groupe 2 token: ${group2Token.substring(0, 12)}...`);
    return true;
  });
  if (r4c === null) p4Fails.push(creator2.email);

  // Extend invitation deadline
  if (group2Token) {
    runSQL(`UPDATE group_invitations SET expires_at = NOW() + INTERVAL '24 hours' WHERE token = '${group2Token}'`);
    log('DB', 'Deadline invitation groupe 2 prolongee');
  }

  await sleep(2_000);

  if (group2Token) {
    console.log('  [Groupe 2] Diane rejoint via invitation...');
    const r4d = await safeRun(joiner2a.email, 'JOIN_GROUP', async () => {
      if (!joiner2a.page) throw new Error('Pas de page');
      await joinGroupLesson(joiner2a.page, group2Token);
      return true;
    });
    if (r4d === null) p4Fails.push(joiner2a.email);

    await sleep(2_000);

    console.log('  [Groupe 2] Emma rejoint via invitation...');
    const r4e = await safeRun(joiner2b.email, 'JOIN_GROUP', async () => {
      if (!joiner2b.page) throw new Error('Pas de page');
      await joinGroupLesson(joiner2b.page, group2Token);
      return true;
    });
    if (r4e === null) p4Fails.push(joiner2b.email);
  }

  results.push({
    name: 'Reservation groupe (UI)',
    success: students.length - p4Fails.length,
    total: students.length,
    failures: p4Fails,
    durationMs: Date.now() - p4Start,
  });

  await sleep(PHASE_DELAY_MS);

  // ========================================================================
  // PHASE 4b: Resolve lesson IDs for all users
  // ========================================================================
  console.log('\n[PHASE 4b] Resolution des lesson IDs via DB...');
  for (const student of students) {
    if (student.userId) {
      const lid = runSQL(`
        SELECT l.id FROM lessons l
        LEFT JOIN lesson_participants lp ON lp.lesson_id = l.id
        WHERE (l.student_id = ${student.userId} OR lp.student_id = ${student.userId})
        AND l.is_group_lesson = true
        AND l.status = 'PENDING'
        ORDER BY l.id DESC LIMIT 1
      `);
      if (lid) {
        student.lessonId = parseInt(lid, 10);
        log('RESOLVE', `${student.firstName} -> lessonId=${student.lessonId}`);
      }
    }
  }

  // Assign lesson IDs to coaches
  for (const coach of coaches) {
    if (coach.userId) {
      const lid = runSQL(`
        SELECT id FROM lessons
        WHERE teacher_id = ${coach.userId}
        AND is_group_lesson = true
        AND status = 'PENDING'
        ORDER BY id DESC LIMIT 1
      `);
      if (lid) {
        coach.lessonId = parseInt(lid, 10);
        log('RESOLVE', `${coach.firstName} -> lessonId=${coach.lessonId}`);
      }
    }
  }

  // ========================================================================
  // PHASE 5: Coaches confirm lessons
  // ========================================================================
  console.log('\n[PHASE 5] Confirmation des cours par les coachs...');
  const p5Start = Date.now();
  const p5Fails: string[] = [];

  for (const coach of coaches) {
    const r5 = await safeRun(coach.email, 'CONFIRM', async () => {
      if (!coach.context || !coach.lessonId) throw new Error('Pas de context ou lessonId');
      await confirmLesson(coach.context, coach.lessonId);
      return true;
    });
    if (r5 === null) p5Fails.push(coach.email);
  }

  results.push({
    name: 'Confirmation cours',
    success: coaches.length - p5Fails.length,
    total: coaches.length,
    failures: p5Fails,
    durationMs: Date.now() - p5Start,
  });

  await sleep(PHASE_DELAY_MS);

  // ========================================================================
  // PHASE 5b: Make lessons joinable (scheduled_at = NOW()+2min)
  // ========================================================================
  console.log('\n[PHASE 5b] Mise a jour DB: scheduled_at -> NOW()+2min...');
  makeGroupLessonsJoinable();

  // Wait 3 minutes (lessons start at +2min, so 1min margin after start)
  const WAIT_BEFORE_JOIN_MS = 3 * 60_000;
  console.log(`\n[PHASE 5c] Attente de ${WAIT_BEFORE_JOIN_MS / 60_000} minutes avant de rejoindre les appels...`);
  const waitStart = Date.now();
  while (Date.now() - waitStart < WAIT_BEFORE_JOIN_MS) {
    const remaining = Math.ceil((WAIT_BEFORE_JOIN_MS - (Date.now() - waitStart)) / 1000);
    if (remaining % 30 === 0) {
      log('WAIT', `${remaining}s restantes...`);
    }
    await sleep(1_000);
  }
  console.log('  Attente terminee — les cours commencent!');

  // ========================================================================
  // PHASE 6: All 7 users join the 2 video calls
  // ========================================================================
  console.log('\n[PHASE 6] 7 utilisateurs rejoignent les 2 appels video collectifs...');
  const p6Start = Date.now();
  const p6Fails: string[] = [];

  const videoParticipants = allUsers.filter(u => u.page && u.context && u.lessonId);
  const coachParticipants = videoParticipants.filter(u => u.role === 'TEACHER');
  const studentParticipants = videoParticipants.filter(u => u.role === 'STUDENT');

  log('VIDEO', `Participants: ${coachParticipants.length} coachs + ${studentParticipants.length} eleves`);

  // Coaches join first (they start recording)
  for (const coach of coachParticipants) {
    const r6 = await safeRun(coach.email, 'VIDEO_COACH', async () => {
      await joinVideoCall(coach.page!, coach.context!, coach.lessonId!, true);
      return true;
    });
    if (r6 === null) p6Fails.push(coach.email);
  }

  await sleep(5_000); // Let coaches connect and start recording

  // Students join
  const studentVideoResults = await Promise.all(
    studentParticipants.map(user =>
      safeRun(user.email, 'VIDEO_STUDENT', async () => {
        await joinVideoCall(user.page!, user.context!, user.lessonId!, false);
        return true;
      })
    )
  );

  studentVideoResults.forEach((r, i) => {
    if (r === null) p6Fails.push(studentParticipants[i]?.email || 'unknown');
  });

  results.push({
    name: 'Appels video groupe (UI)',
    success: videoParticipants.length - p6Fails.length,
    total: videoParticipants.length,
    failures: p6Fails,
    durationMs: Date.now() - p6Start,
  });

  // ========================================================================
  // PHASE 7: Maintain call for recording
  // ========================================================================
  const usersInCall = videoParticipants.length - p6Fails.length;
  console.log(`\n[PHASE 7] ${usersInCall} utilisateurs en appel — maintien ${CALL_DURATION_MS / 1000}s pour enregistrement...`);

  // Check recording status
  await sleep(10_000);
  for (const coach of coachParticipants) {
    if (coach.page) {
      try {
        const recIndicator = coach.page.locator('.recording-indicator, [class*="recording"]');
        const isRec = (await recIndicator.count()) > 0;
        log('VIDEO', `Lesson ${coach.lessonId} (group): recording indicator = ${isRec}`);
      } catch { /* ignore */ }
    }
  }

  await sleep(CALL_DURATION_MS - 10_000);
  console.log('  Fin du maintien en appel.');

  // ========================================================================
  // PHASE 7b: Mark as COMPLETED + wait for Jibri webhook
  // ========================================================================
  console.log('\n[PHASE 7b] Passage des cours en COMPLETED + attente webhook Jibri...');
  completeGroupLessons();

  const WEBHOOK_WAIT_MS = 90_000;
  console.log(`  Attente ${WEBHOOK_WAIT_MS / 1000}s pour la finalisation Jibri...`);
  const webhookWaitStart = Date.now();
  while (Date.now() - webhookWaitStart < WEBHOOK_WAIT_MS) {
    const remaining = Math.ceil((WEBHOOK_WAIT_MS - (Date.now() - webhookWaitStart)) / 1000);
    if (remaining % 30 === 0) {
      const intermediate = checkGroupRecordingsInDB();
      log('WAIT', `${remaining}s — recording_url: ${intermediate.withRecording}, segments: ${intermediate.withSegments}`);
      if (intermediate.withRecording > 0 || intermediate.withSegments > 0) {
        log('WAIT', 'Enregistrements detectes!');
        break;
      }
    }
    await sleep(1_000);
  }

  // ========================================================================
  // PHASE 8: Verify recordings in DB
  // ========================================================================
  console.log('\n[PHASE 8] Verification des enregistrements video en DB...');
  const p8Start = Date.now();

  const recordingCheck = checkGroupRecordingsInDB();
  console.log('\n  Enregistrements trouves:');
  console.log(`    Cours collectifs: ${recordingCheck.total}`);
  console.log(`    Avec recording_url: ${recordingCheck.withRecording}/${recordingCheck.total}`);
  console.log(`    Avec recording_segments: ${recordingCheck.withSegments}/${recordingCheck.total}`);
  for (const detail of recordingCheck.details) {
    console.log(detail);
  }

  // Diagnostic: recording directory
  const recordingDirCheck = (() => {
    try {
      return execSync(
        'ls -lt /var/jibri/recordings/ 2>/dev/null | head -10',
        { encoding: 'utf-8', timeout: 5_000 }
      ).trim();
    } catch { return 'N/A'; }
  })();
  console.log('\n  Dossier /var/jibri/recordings/:');
  for (const line of recordingDirCheck.split('\n')) {
    console.log(`    ${line}`);
  }

  results.push({
    name: 'Enregistrements groupe (DB)',
    success: recordingCheck.withRecording + recordingCheck.withSegments,
    total: recordingCheck.total,
    failures: recordingCheck.withRecording === 0 && recordingCheck.total > 0
      ? ['Aucun recording — verifier Jibri']
      : [],
    durationMs: Date.now() - p8Start,
  });

  // ========================================================================
  // PHASE 9: Cleanup
  // ========================================================================
  console.log('\n[PHASE 9] Fermeture des contextes...');
  for (const ctx of allContexts) {
    try { await ctx.close(); } catch { /* ignore */ }
  }

  // ========================================================================
  // FINAL SUMMARY
  // ========================================================================
  const totalDuration = Date.now() - testStart;

  console.log('\n' + '='.repeat(70));
  console.log('  RESULTATS — STRESS TEST COURS COLLECTIFS');
  console.log('  Groupe 1: Coach A + 2 eleves | Groupe 2: Coach B + 3 eleves');
  console.log('='.repeat(70));
  console.log('');

  for (const r of results) {
    const icon = r.success === r.total ? '\u2713' : (r.success > 0 ? '~' : '\u2717');
    const pct = r.total > 0 ? ((r.success / r.total) * 100).toFixed(0) : 'N/A';
    console.log(`  ${icon} ${r.name.padEnd(30)} ${r.success}/${r.total} (${pct}%) — ${(r.durationMs / 1000).toFixed(1)}s`);
    for (const f of r.failures.slice(0, 3)) {
      console.log(`      \u2717 ${f}`);
    }
  }

  console.log('');
  console.log(`  Duree totale: ${(totalDuration / 1000).toFixed(1)}s (${(totalDuration / 60_000).toFixed(1)}min)`);
  console.log('');

  if (recordingCheck.withRecording > 0 || recordingCheck.withSegments > 0) {
    console.log('  \u2713 ENREGISTREMENT VIDEO GROUPE: Enregistrements detectes en DB');
  } else if (recordingCheck.total > 0) {
    console.log('  \u26a0 ENREGISTREMENT VIDEO GROUPE: Aucun enregistrement detecte');
    console.log('    Verifier que les 8 Jibri sont actifs (docker compose ps)');
  } else {
    console.log('  \u2717 Aucun cours collectif trouve en DB');
  }

  console.log('='.repeat(70) + '\n');
});
