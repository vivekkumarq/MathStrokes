import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  OnDestroy,
  computed,
  effect,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Chart, ArcElement, DoughnutController, Legend, Tooltip } from 'chart.js';

import { ApiFailure, AttemptResultResponse, toApiFailure } from '../../core';
import { Logo } from '../../shared/brand/logo';
import { ExamService } from '../exam/exam.service';

Chart.register(DoughnutController, ArcElement, Legend, Tooltip);

/**
 * The result dashboard.
 *
 * Rank is deliberately re-fetched when the tab regains focus. It is a snapshot: a student who
 * finished first is genuinely rank 1 of 1 at that moment and may be rank 12 of 40 an hour
 * later, while their score never moves. Showing a stale rank would be quietly wrong.
 */
@Component({
  selector: 'app-result-page',
  imports: [RouterLink, Logo],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './result-page.html',
  styleUrl: './result-page.scss',
})
export class ResultPage implements AfterViewInit, OnDestroy {
  private readonly exam = inject(ExamService);
  private readonly route = inject(ActivatedRoute);

  private readonly canvas = viewChild<ElementRef<HTMLCanvasElement>>('breakdown');
  private chart?: Chart;

  protected readonly attemptId = Number(this.route.snapshot.paramMap.get('attemptId'));
  protected readonly result = signal<AttemptResultResponse | null>(null);
  protected readonly loading = signal(true);
  protected readonly failure = signal<ApiFailure | null>(null);

  /** Score as a share of the paper, floored at zero: negative marking can push a total below it. */
  protected readonly scorePercent = computed(() => {
    const r = this.result();
    if (!r?.score || !r.maxScore) {
      return 0;
    }
    return Math.max(0, Math.round((r.score / r.maxScore) * 100));
  });

  protected readonly timeTaken = computed(() => {
    const seconds = this.result()?.timeTakenSeconds;
    if (seconds == null) {
      return '—';
    }
    const minutes = Math.floor(seconds / 60);
    const rest = seconds % 60;
    return `${minutes}m ${String(rest).padStart(2, '0')}s`;
  });

  private readonly refreshOnFocus = () => this.load();

  constructor() {
    this.load();
    window.addEventListener('focus', this.refreshOnFocus);
    // Redraw whenever the numbers change, including after a focus refresh.
    effect(() => {
      const r = this.result();
      if (r) {
        this.draw(r);
      }
    });
  }

  ngAfterViewInit(): void {
    const r = this.result();
    if (r) {
      this.draw(r);
    }
  }

  ngOnDestroy(): void {
    window.removeEventListener('focus', this.refreshOnFocus);
    this.chart?.destroy();
  }

  protected load(): void {
    this.exam.result(this.attemptId).subscribe({
      next: (result) => {
        this.result.set(result);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.failure.set(toApiFailure(error));
        this.loading.set(false);
      },
    });
  }

  private draw(result: AttemptResultResponse): void {
    const element = this.canvas()?.nativeElement;
    if (!element) {
      return;
    }
    const data = [
      result.correctCount ?? 0,
      result.partiallyCorrectCount ?? 0,
      result.incorrectCount ?? 0,
      result.unansweredCount ?? 0,
    ];
    if (this.chart) {
      this.chart.data.datasets[0].data = data;
      this.chart.update();
      return;
    }
    this.chart = new Chart(element, {
      type: 'doughnut',
      data: {
        labels: ['Correct', 'Partially correct', 'Incorrect', 'Unanswered'],
        datasets: [
          {
            data,
            backgroundColor: ['#16a34a', '#0891b2', '#dc2626', '#cbd5e1'],
            borderWidth: 0,
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        cutout: '62%',
        plugins: {
          legend: { position: 'bottom', labels: { boxWidth: 12, padding: 14 } },
        },
      },
    });
  }
}
