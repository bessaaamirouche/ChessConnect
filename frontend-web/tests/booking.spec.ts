import { test, expect } from '@playwright/test';

test.describe('Réservation de cours', () => {

  test('redirige vers login si non connecté', async ({ page }) => {
    // Essaie d'accéder à la page de réservation directement
    await page.goto('/book/1');

    // Devrait rediriger vers login ou afficher un message
    await page.waitForLoadState('networkidle');

    const isOnLogin = page.url().includes('login');
    const hasLoginPrompt = await page.getByText(/connexion|connecter|login/i).first().isVisible().catch(() => false);

    expect(isOnLogin || hasLoginPrompt).toBeTruthy();
  });

  test('la page de réservation publique affiche les informations', async ({ page }) => {
    // Va sur la liste des coachs
    await page.goto('/teachers');
    await page.waitForLoadState('networkidle');

    // Clique sur réserver pour le premier coach
    const bookButton = page.getByRole('link', { name: /réserver/i }).first();

    if (await bookButton.isVisible()) {
      await bookButton.click();
      await page.waitForLoadState('networkidle');

      // Soit on est sur la page de réservation, soit redirigé vers login/register
      const url = page.url();
      const isBookingPage = url.includes('book');
      const isAuthPage = url.includes('login') || url.includes('register');

      expect(isBookingPage || isAuthPage).toBeTruthy();
    }
  });
});

test.describe('Réservation (utilisateur connecté)', () => {
  // Ces tests nécessitent un utilisateur de test connecté
  // Ils sont marqués comme skip par défaut car ils nécessitent une authentification réelle

  test.skip('affiche les créneaux disponibles', async ({ page }) => {
    // TODO: Implémenter avec un utilisateur de test authentifié
    await page.goto('/book/1');
    await page.waitForLoadState('networkidle');

    // Devrait afficher les créneaux ou un message "aucun créneau"
    const slots = page.locator('[class*="slot"], [class*="creneau"]');
    const noSlots = page.getByText(/aucun créneau|pas de disponibilité/i);

    const hasSlots = await slots.first().isVisible().catch(() => false);
    const hasNoSlotsMessage = await noSlots.isVisible().catch(() => false);

    expect(hasSlots || hasNoSlotsMessage).toBeTruthy();
  });

  test.skip('permet de sélectionner un créneau', async ({ page }) => {
    // TODO: Implémenter avec un utilisateur de test authentifié
    await page.goto('/book/1');
    await page.waitForLoadState('networkidle');

    const firstSlot = page.locator('[class*="slot"]:not([class*="unavailable"])').first();

    if (await firstSlot.isVisible()) {
      await firstSlot.click();

      // Le créneau devrait être sélectionné
      await expect(firstSlot).toHaveClass(/selected|active/i);
    }
  });

  test.skip('affiche le résumé du prix', async ({ page }) => {
    // TODO: Implémenter avec un utilisateur de test authentifié
    await page.goto('/book/1');
    await page.waitForLoadState('networkidle');

    // Devrait afficher le prix
    await expect(page.getByText(/€|prix|tarif/i).first()).toBeVisible();
  });
});
