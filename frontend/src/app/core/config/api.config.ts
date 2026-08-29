import { InjectionToken } from '@angular/core';

export interface ApiConfig {
  /** Includes the server's context-path. e.g. /auth/login resolves to <baseUrl>/auth/login */
  baseUrl: string;
  /**
   * Refresh this many seconds BEFORE the access token actually expires, so a request is
   * never sent with a token that dies in flight.
   */
  refreshSkewSeconds: number;
}

export const API_CONFIG = new InjectionToken<ApiConfig>('API_CONFIG');

/**
 * Backend runs on 8080 with context-path /api; it already allows CORS from
 * http://localhost:4200 with the Authorization header exposed.
 */
export const DEFAULT_API_CONFIG: ApiConfig = {
  baseUrl: 'http://localhost:8080/api',
  refreshSkewSeconds: 30,
};
