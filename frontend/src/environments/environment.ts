/**
 * Development configuration.
 *
 * The API base URL is ABSOLUTE here on purpose: `ng serve` runs no proxy, so a relative
 * '/api' would resolve against :4200 and 404. The backend allows CORS from :4200.
 *
 * Swapped for environment.prod.ts at production build time via fileReplacements in
 * angular.json — do not import the .prod file directly anywhere.
 */
export const environment = {
  production: false,
  apiBaseUrl: 'http://localhost:8080/api',
};
