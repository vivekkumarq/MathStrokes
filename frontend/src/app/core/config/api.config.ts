import { InjectionToken } from '@angular/core';

import { environment } from '../../../environments/environment';

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
 * Base URL comes from the environment file, which angular.json swaps at production build
 * time. Never hardcode a host here: a compiled-in localhost makes every deployed
 * visitor's browser try to reach a backend on their own machine.
 *
 * dev  -> http://localhost:8080/api  (absolute; ng serve runs no proxy)
 * prod -> /api                       (relative; the host proxies to the backend)
 */
export const DEFAULT_API_CONFIG: ApiConfig = {
  baseUrl: environment.apiBaseUrl,
  refreshSkewSeconds: 30,
};
