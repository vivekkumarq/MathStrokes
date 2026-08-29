import { RoleName } from './enums';

/**
 * Auth contract as confirmed by the backend session.
 *
 * The account key is the PHONE NUMBER. There is no email field anywhere in the system.
 * Password recovery is a three-step security-question flow, not a mailed link.
 *
 * Property names here are the exact JSON body property names, which is also what
 * `fieldErrors[].field` returns — so server validation binds straight onto the form.
 */

export interface LoginRequest {
  phoneNumber: string;
  password: string;
}

export interface StudentRegisterRequest {
  fullName: string;
  phoneNumber: string;
  password: string;
  confirmPassword: string;
  securityQuestion: string;
  securityAnswer: string;
}

/** The authenticated principal. Roles drive routing and menu visibility. */
export interface AuthUser {
  id: number;
  fullName: string;
  phoneNumber: string;
  roles: RoleName[];
  enabled: boolean;
  /** ISO-8601 instant. */
  createdAt: string;
}

/**
 * Short-lived access token plus a persisted rotating refresh token.
 *
 * Refresh ROTATES BOTH tokens and the server revokes the old refresh token the moment
 * it is used, so every field here must be replaced on each refresh — never merged.
 *
 * Expiry is tracked from `expiresInSeconds` rather than by decoding the JWT, so the
 * client never has to parse a token it does not own.
 */
export interface AuthTokens {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresInSeconds: number;
}

export type AuthResponse = AuthTokens & { user: AuthUser };

export interface RefreshRequest {
  refreshToken: string;
}

export interface LogoutRequest {
  refreshToken: string;
}

// --- Forgot password: three steps, each one a separate endpoint ----------------------

/** One entry of the canonical list from GET /auth/security-questions. */
export interface SecurityQuestion {
  id: string;
  text: string;
}

/** Step 1. Returns ONLY the security question; knowing a phone number alone gets you nothing. */
export interface ForgotPasswordInitiateRequest {
  phoneNumber: string;
}

export interface ForgotPasswordInitiateResponse {
  securityQuestion: string;
}

/** Step 2. Correct answer yields a short-lived single-use reset token. */
export interface ForgotPasswordVerifyRequest {
  phoneNumber: string;
  securityAnswer: string;
}

export interface ForgotPasswordVerifyResponse {
  resetToken: string;
}

/** Step 3. */
export interface ResetPasswordRequest {
  resetToken: string;
  newPassword: string;
  confirmPassword: string;
}

export function hasRole(user: AuthUser | null, role: RoleName): boolean {
  return user?.roles.includes(role) ?? false;
}

export function isAdmin(user: AuthUser | null): boolean {
  return hasRole(user, 'ROLE_ADMIN');
}

export function isStudent(user: AuthUser | null): boolean {
  return hasRole(user, 'ROLE_STUDENT');
}
