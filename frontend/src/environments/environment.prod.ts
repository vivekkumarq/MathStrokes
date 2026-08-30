/**
 * Production configuration.
 *
 * ABSOLUTE, calling Render directly rather than the relative /api proxy.
 *
 * The proxy was the original design and it removed CORS entirely, which was a real
 * benefit. It also introduced a fatal one: Netlify's proxy gives up at ~29 seconds, and
 * a cold JVM boot on this hosting tier takes 30-50. So every request made while the
 * backend was asleep returned 504 and the site was completely unusable until something
 * else happened to wake it — verified in production, three consecutive 504s at 28.9s,
 * 29.8s and 28.5s, all of them Netlify's ceiling rather than the backend failing.
 *
 * Going direct removes that ceiling. The browser's own timeout is minutes, so a cold
 * start is now slow but successful instead of a hard failure. The cost is a CORS
 * preflight of a few milliseconds; the backend already allows this origin.
 */
export const environment = {
  production: true,
  apiBaseUrl: 'https://iota-api-jjai.onrender.com/api',
};
