import { Injectable } from '@angular/core';

/**
 * Service to validate URLs before redirecting to prevent open redirect vulnerabilities.
 * Only allows redirects to trusted domains (Stripe, same-origin, etc.)
 */
@Injectable({
  providedIn: 'root'
})
export class UrlValidatorService {

  // Trusted external domains for redirects
  private readonly trustedDomains: string[] = [
    'checkout.stripe.com',
    'js.stripe.com',
    'connect.stripe.com',
    'dashboard.stripe.com',
    'meet.jit.si',
    'meet.mychess.fr',
    'mychess.fr',
    'www.mychess.fr'
  ];

  /**
   * Validate if a URL is safe for redirection.
   * @param url The URL to validate
   * @returns true if the URL is safe to redirect to
   */
  isValidRedirectUrl(url: string | null | undefined): boolean {
    if (!url) {
      return false;
    }

    try {
      const parsedUrl = new URL(url, window.location.origin);

      // Allow same-origin URLs
      if (parsedUrl.origin === window.location.origin) {
        return true;
      }

      // Only allow HTTPS for external URLs
      if (parsedUrl.protocol !== 'https:') {
        console.warn('Blocked redirect to non-HTTPS URL:', url);
        return false;
      }

      // Check if domain is in trusted list
      const hostname = parsedUrl.hostname.toLowerCase();
      const isTrusted = this.trustedDomains.some(domain =>
        hostname === domain || hostname.endsWith('.' + domain)
      );

      if (!isTrusted) {
        console.warn('Blocked redirect to untrusted domain:', hostname);
        return false;
      }

      return true;
    } catch (e) {
      console.error('Invalid URL for redirect:', url);
      return false;
    }
  }

  /**
   * Safely redirect to a URL if it passes validation.
   * @param url The URL to redirect to
   * @param fallbackPath Path to navigate to if URL is invalid (default: home)
   * @returns true if redirect was performed, false if blocked
   */
  safeRedirect(url: string | null | undefined, fallbackPath: string = '/'): boolean {
    if (this.isValidRedirectUrl(url)) {
      window.location.href = url!;
      return true;
    }

    // Navigate to fallback path if URL is invalid
    console.warn('Redirect blocked, navigating to fallback:', fallbackPath);
    window.location.href = fallbackPath;
    return false;
  }

  /**
   * Validate a Stripe checkout URL specifically.
   */
  isValidStripeUrl(url: string | null | undefined): boolean {
    if (!url) return false;

    try {
      const parsedUrl = new URL(url);
      const isStripe = parsedUrl.hostname === 'checkout.stripe.com' ||
                       parsedUrl.hostname === 'connect.stripe.com' ||
                       parsedUrl.hostname.endsWith('.stripe.com');
      const isHttps = parsedUrl.protocol === 'https:';

      return isStripe && isHttps;
    } catch {
      return false;
    }
  }
}
