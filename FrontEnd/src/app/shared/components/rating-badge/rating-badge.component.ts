import { CommonModule } from '@angular/common';
import { Component, inject, input, OnInit, signal } from '@angular/core';

import { LoanService } from '../../../core/services/loan.service';
import { RatingSummary } from '../../../core/models/review.models';

/** Media de valoraciones de un usuario ("★ 4,6 · 12 valoraciones"). Opcionalmente, los últimos comentarios. */
@Component({
  selector: 'app-rating-badge',
  standalone: true,
  imports: [CommonModule],
  template: `
    @if (summary(); as s) {
      @if (s.count > 0) {
        <span class="inline-flex items-center gap-1 text-sm font-bold text-text-main">
          <span class="material-symbols-outlined text-amber-500" style="font-size:18px;">star</span>
          {{ s.average | number:'1.1-1' }}
          <span class="font-normal text-text-secondary">· {{ s.count }} {{ s.count === 1 ? 'valoración' : 'valoraciones' }}</span>
        </span>
        @if (showComments() && s.recent.length > 0) {
          <ul class="mt-3 space-y-2">
            @for (c of s.recent; track $index) {
              <li class="text-sm text-text-main">
                <span class="text-amber-500">{{ stars(c.rating) }}</span> {{ c.comment }}
              </li>
            }
          </ul>
        }
      } @else {
        <span class="text-xs text-text-secondary">Sin valoraciones todavía</span>
      }
    }
  `,
})
export class RatingBadgeComponent implements OnInit {
  private readonly loanService = inject(LoanService);

  readonly userId = input.required<number>();
  readonly showComments = input(false);
  protected readonly summary = signal<RatingSummary | null>(null);

  ngOnInit(): void {
    this.loanService.getRating(this.userId()).subscribe({
      next: (s) => this.summary.set(s),
      error: () => this.summary.set(null),
    });
  }

  protected stars(n: number): string {
    return '★'.repeat(n) + '☆'.repeat(5 - n);
  }
}
