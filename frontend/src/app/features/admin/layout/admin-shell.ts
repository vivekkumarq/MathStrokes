import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

import { Logo } from '../../../shared/brand/logo';

import { AuthService } from '../../../core/auth/auth.service';
import { AuthStore } from '../../../core/auth/auth.store';

/**
 * Chrome shared by every admin screen. Presentational: pages project their own content,
 * so the admin section needs no nested router-outlet.
 */
@Component({
  selector: 'app-admin-shell',
  imports: [Logo, RouterLink, RouterLinkActive],
  template: `
    <div class="shell">
      <header class="bar">
        <div class="left">
          <a class="brand" routerLink="/admin"><app-logo [size]="32" /><span class="badge">Admin</span></a>
          <nav>
            <a routerLink="/admin/questions" routerLinkActive="active">Questions</a>
            <a routerLink="/admin/tests" routerLinkActive="active">Tests</a>
          </nav>
        </div>
        <div class="who">
          <span>{{ store.displayName() }}</span>
          <button class="ms-btn ms-btn--ghost" type="button" (click)="signOut()">Sign out</button>
        </div>
      </header>
      <main class="content">
        <ng-content />
      </main>
    </div>
  `,
  styles: `
    .shell {
      min-height: 100dvh;
    }
    .bar {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 24px;
      padding: 0 24px;
      background: var(--ms-surface);
      border-bottom: 1px solid var(--ms-border);
    }
    .left {
      display: flex;
      align-items: center;
      gap: 28px;
    }
    .brand {
      display: flex;
      align-items: center;
      gap: 10px;
      text-decoration: none;
      padding: 13px 0;
    }
    .badge {
      padding: 3px 8px;
      font-size: 10.5px;
      font-weight: 700;
      letter-spacing: 0.07em;
      text-transform: uppercase;
      color: var(--ms-primary-strong);
      background: var(--ms-primary-soft);
      border: 1px solid var(--ms-primary-border);
      border-radius: 999px;
    }
    nav {
      display: flex;
      gap: 4px;
    }
    nav a {
      padding: 17px 12px;
      font-size: 14px;
      font-weight: 500;
      color: var(--ms-ink-muted);
      text-decoration: none;
      border-bottom: 2px solid transparent;
    }
    nav a:hover {
      color: var(--ms-ink);
      text-decoration: none;
    }
    nav a.active {
      color: var(--ms-primary);
      border-bottom-color: var(--ms-primary);
    }
    .who {
      display: flex;
      gap: 14px;
      align-items: center;
      font-size: 14px;
      color: var(--ms-ink-muted);
    }
    .content {
      max-width: 1180px;
      margin: 0 auto;
      padding: 24px;
    }
  `,
})
export class AdminShell {
  protected readonly store = inject(AuthStore);
  private readonly auth = inject(AuthService);

  protected signOut(): void {
    this.auth.logout().subscribe({
      next: () => this.store.clearSession(),
      error: () => this.store.clearSession(),
    });
  }
}
