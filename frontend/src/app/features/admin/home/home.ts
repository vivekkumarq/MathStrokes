import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { AdminDashboardResponse } from '../../../core/models';
import { AdminAnalyticsService } from '../data/admin-analytics.service';
import { AdminShell } from '../layout/admin-shell';

@Component({
  selector: 'app-admin-home',
  imports: [AdminShell, RouterLink],
  templateUrl: './home.html',
  styleUrl: './home.scss',
})
export class AdminHome {
  private readonly analytics = inject(AdminAnalyticsService);

  protected readonly stats = signal<AdminDashboardResponse | null>(null);

  constructor() {
    this.analytics.dashboard().subscribe({
      next: (stats) => this.stats.set(stats),
      // The page is still useful without counters — the links are the point.
      error: () => undefined,
    });
  }
}
