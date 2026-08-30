/**
 * Production configuration.
 *
 * RELATIVE on purpose: the host proxies /api/* through to the backend, so the browser
 * calls the app's own origin. That means no CORS preflight, no cross-origin config to
 * keep in step, and the backend hostname lives in the host config rather than being
 * compiled into the bundle.
 */
export const environment = {
  production: true,
  apiBaseUrl: '/api',
};
