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
    title: 'Sign in · iota',
    loadComponent: () => import('./features/auth/login/login').then((m) => m.Login),
  },
  {
    path: 'register',
    canActivate: [guestGuard],
    title: 'Create account · iota',
    loadComponent: () => import('./features/auth/register/register').then((m) => m.Register),
  },
  {
    path: 'forgot-password',
    canActivate: [guestGuard],
    title: 'Reset password · iota',
    loadComponent: () =>
      import('./features/auth/forgot-password/forgot-password').then((m) => m.ForgotPassword),
  },
  {
    path: 'student',
    canActivate: [roleGuard('ROLE_STUDENT')],
    title: 'Dashboard · iota',
    loadComponent: () => import('./features/student/home/home').then((m) => m.StudentHome),
  },
  {
    path: 'admin',
    canActivate: [roleGuard('ROLE_ADMIN')],
    title: 'Admin · iota',
    loadComponent: () => import('./features/admin/home/home').then((m) => m.AdminHome),
  },
  {
    path: 'student/tests',
    canActivate: [roleGuard('ROLE_STUDENT')],
    title: 'Available tests · iota',
    loadComponent: () =>
      import('./features/student/tests/test-list').then((m) => m.TestList),
  },
  {
    path: 'exam/:attemptId',
    canActivate: [roleGuard('ROLE_STUDENT')],
    title: 'Examination · iota',
    loadComponent: () => import('./features/exam/exam-runner').then((m) => m.ExamRunner),
  },
  {
    // Declared before the review child so neither swallows the other; Angular matches in
    // declaration order and both share the /results/:attemptId prefix.
    path: 'results/:attemptId',
    pathMatch: 'full',
    canActivate: [roleGuard('ROLE_STUDENT')],
    title: 'Result · iota',
    loadComponent: () => import('./features/results/result-page').then((m) => m.ResultPage),
  },
  {
    path: 'results/:attemptId/review',
    canActivate: [roleGuard('ROLE_STUDENT')],
    title: 'Answer review · iota',
    loadComponent: () => import('./features/results/review-page').then((m) => m.ReviewPage),
  },
  {
    path: 'admin/students',
    canActivate: [roleGuard('ROLE_ADMIN')],
    title: 'Students · iota',
    loadComponent: () =>
      import('./features/admin/students/student-list').then((m) => m.AdminStudentList),
  },
  {
    path: 'admin/tests',
    canActivate: [roleGuard('ROLE_ADMIN')],
    title: 'Tests · iota',
    loadComponent: () =>
      import('./features/admin/tests/test-list').then((m) => m.AdminTestList),
  },
  {
    path: 'admin/questions',
    canActivate: [roleGuard('ROLE_ADMIN')],
    title: 'Question bank · iota',
    loadComponent: () =>
      import('./features/admin/questions/question-list/question-list').then((m) => m.QuestionList),
  },
  {
    // Must precede the :id route, or 'new' would be parsed as an id.
    path: 'admin/questions/new',
    canActivate: [roleGuard('ROLE_ADMIN')],
    title: 'New question · iota',
    loadComponent: () =>
      import('./features/admin/questions/question-editor/question-editor').then(
        (m) => m.QuestionEditor,
      ),
  },
  {
    path: 'admin/questions/:id',
    canActivate: [roleGuard('ROLE_ADMIN')],
    title: 'Edit question · iota',
    loadComponent: () =>
      import('./features/admin/questions/question-editor/question-editor').then(
        (m) => m.QuestionEditor,
      ),
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
