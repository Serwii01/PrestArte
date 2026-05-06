import { CommonModule } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';

import { AuthService } from '../../../core/services/auth.service';
import { LoanService } from '../../../core/services/loan.service';
import { LOAN_STATUS_LABEL, LoanResponse } from '../../../core/models/loan.models';

@Component({
  selector: 'app-foundation-dashboard',
  standalone: true,
  imports: [CommonModule],
  template: `
    <h1 class="text-2xl font-semibold mb-6">Mi fundación</h1>

    <section class="bg-white rounded-xl shadow p-6">
      <h2 class="text-lg font-medium mb-4">Mis solicitudes de préstamo</h2>

      @if (loading()) {
        <p class="text-slate-500">Cargando...</p>
      } @else if (loans().length === 0) {
        <p class="text-slate-500">Aún no has enviado solicitudes.</p>
      } @else {
        <ul class="divide-y divide-slate-100">
          @for (l of loans(); track l.id) {
            <li class="py-3">
              <div class="flex items-center justify-between">
                <div>
                  <p class="font-medium">{{ l.artworkTitle }}</p>
                  <p class="text-xs text-slate-400">
                    Coleccionista: {{ l.collectorName }} · {{ l.startDate }} → {{ l.endDate }}
                  </p>
                </div>
                <span class="inline-block px-2 py-0.5 rounded bg-slate-100 text-slate-700 text-sm">
                  {{ statusLabel(l.status) }}
                </span>
              </div>
            </li>
          }
        </ul>
      }
    </section>
  `,
})
export class FoundationDashboardComponent implements OnInit {
  private readonly loanService = inject(LoanService);
  private readonly auth = inject(AuthService);

  protected readonly loans = signal<LoanResponse[]>([]);
  protected readonly loading = signal(true);

  ngOnInit(): void {
    const foundationId = this.auth.userId();
    if (foundationId == null) return;

    this.loanService.getByFoundation(foundationId).subscribe({
      next: (loans) => {
        this.loans.set(loans);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  statusLabel(s: LoanResponse['status']): string {
    return LOAN_STATUS_LABEL[s] ?? s;
  }
}
