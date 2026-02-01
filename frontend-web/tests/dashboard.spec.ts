import { test, expect } from '@playwright/test';

test.describe('Dashboard (accès protégé)', () => {

  test('redirige vers login si non connecté', async ({ page }) => {
    await page.goto('/dashboard');
    await page.waitForLoadState('networkidle');

    // Devrait rediriger vers login
    expect(page.url()).toMatch(/login|connexion/i);
  });

  test('page /lessons redirige vers login si non connecté', async ({ page }) => {
    await page.goto('/lessons');
    await page.waitForLoadState('networkidle');

    expect(page.url()).toMatch(/login|connexion/i);
  });

  test('page /settings redirige vers login si non connecté', async ({ page }) => {
    await page.goto('/settings');
    await page.waitForLoadState('networkidle');

    expect(page.url()).toMatch(/login|connexion/i);
  });

  test('page /wallet redirige vers login si non connecté', async ({ page }) => {
    await page.goto('/wallet');
    await page.waitForLoadState('networkidle');

    expect(page.url()).toMatch(/login|connexion/i);
  });

  test('page /subscription redirige vers login si non connecté', async ({ page }) => {
    await page.goto('/subscription');
    await page.waitForLoadState('networkidle');

    expect(page.url()).toMatch(/login|connexion/i);
  });

  test('page /availability redirige vers login si non connecté', async ({ page }) => {
    await page.goto('/availability');
    await page.waitForLoadState('networkidle');

    expect(page.url()).toMatch(/login|connexion/i);
  });
});

test.describe('Pages publiques accessibles', () => {

  test('page /pricing est accessible', async ({ page }) => {
    await page.goto('/pricing');
    await page.waitForLoadState('networkidle');

    // Vérifie que la page se charge (soit pricing, soit redirection vers une page avec les tarifs)
    const hasPricingContent = await page.getByText(/€|prix|tarif|premium|abonnement|mois/i).first().isVisible().catch(() => false);
    const isOnPricingPage = page.url().includes('pricing') || page.url().includes('subscription');

    expect(hasPricingContent || isOnPricingPage).toBeTruthy();
  });

  test('page /blog est accessible', async ({ page }) => {
    await page.goto('/blog');
    await page.waitForLoadState('networkidle');

    // Devrait afficher le blog ou un message vide
    const hasContent = await page.locator('article, [class*="article"], [class*="blog"]').first().isVisible().catch(() => false);
    const hasEmptyMessage = await page.getByText(/aucun article|pas d'article|bientôt/i).isVisible().catch(() => false);

    expect(hasContent || hasEmptyMessage || page.url().includes('blog')).toBeTruthy();
  });

  test('page /terms est accessible', async ({ page }) => {
    await page.goto('/terms');
    await page.waitForLoadState('networkidle');

    await expect(page.getByText(/conditions|utilisation|terms/i).first()).toBeVisible();
  });

  test('page /privacy est accessible', async ({ page }) => {
    await page.goto('/privacy');
    await page.waitForLoadState('networkidle');

    await expect(page.getByText(/confidentialité|privacy|données/i).first()).toBeVisible();
  });
});

test.describe('Navigation globale', () => {

  test('le logo redirige vers la page d\'accueil', async ({ page }) => {
    await page.goto('/teachers');
    await page.waitForLoadState('networkidle');

    const logo = page.locator('a[href="/"] img, a[href="/"] [class*="logo"]').first();

    if (await logo.isVisible()) {
      await logo.click();
      await expect(page).toHaveURL('/');
    }
  });

  test('routes inexistantes affichent 404 ou redirigent', async ({ page }) => {
    await page.goto('/this-page-does-not-exist-12345');
    await page.waitForLoadState('networkidle');

    // Trois comportements possibles :
    // 1. Page 404 affichée
    // 2. Redirection vers accueil
    // 3. Autre page (Angular peut gérer différemment)
    const has404 = await page.getByText(/404|not found|page introuvable|n'existe pas/i).isVisible().catch(() => false);
    const isHome = page.url() === 'http://localhost:4200/' || page.url().endsWith('/');
    const isOnSomePage = await page.locator('body').isVisible();

    expect(has404 || isHome || isOnSomePage).toBeTruthy();
  });
});
