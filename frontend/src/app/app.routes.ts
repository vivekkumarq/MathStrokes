import { Routes } from '@angular/router';

import { guestGuard, landingGuard, roleGuard } from './core/guards/auth.guard';

/**
 * Every feature is lazily loaded. The exam runner will pull in KaTeX and the results
 * screen Chart.js; neither belongs in the bundle a student downloads to reach the
 * login screen.
 */
export const routes: Routes = [
  {
    path: 'login',
    canActivate: [guestGuard],
    title: 'Sign in · MathStrokes',
    loadComponent: () => import('./features/auth/login/login').then((m) => m.Login),
  },
  {
    path: 'register',
    canActivate: [guestGuard],
    title: 'Create account · MathStrokes',
    loadComponent: () => import('./features/auth/register/register').then((m) => m.Register),
  },
  {
    path: 'forgot-password',
    canActivate: [guestGuard],
    title: 'Reset password · MathStrokes',
    loadComponent: () =>
      import('./features/auth/forgot-password/forgot-password').then((m) => m.ForgotPassword),
  },
  {
    path: 'student',
    canActivate: [roleGuard('ROLE_STUDENT')],
    title: 'Dashboard · MathStrokes',
    loadComponent: () => import('./features/student/home/home').then((m) => m.StudentHome),
  },
  {
    path: 'admin',
    canActivate: [roleGuard('ROLE_ADMIN')],
    title: 'Admin · MathStrokes',
    loadComponent: () => import('./features/admin/home/home').then((m) => m.AdminHome),
  },
  {
    // Sends an authenticated user to their own dashboard and everyone else to login.
    path: '',
    pathMatch: 'full',
    canActivate: [landingGuard],
    children: [],
  },
  { path: '**', redirectTo: '' },
];
