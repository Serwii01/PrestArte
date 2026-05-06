import { CommonModule } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';

import { AuthService } from '../../../core/services/auth.service';
import { LoanService } from '../../../core/services/loan.service';
import { LOAN_STATUS_LABEL, LoanResponse } from '../../../core/models/loan.models';

@Component({
  selector: 'app-collector-dashboard',
  standalone: true,
  imports: [CommonModule],
  template: `
    <h1 class="text-2xl font-semibold mb-6">Mi colección</h1>

    <section class="bg-white rounded-xl shadow p-6">
      <h2 class="text-lg font-medium mb-4">Solicitudes recibidas</h2>

      @if (loading()) {
        <p class="text-slate-500">Cargando...</p>
      } @else if (loans().length === 0) {
        <p class="text-slate-500">No tienes solicitudes activas.</p>
      } @else {
        <table class="w-full text-sm">
          <thead class="text-left text-slate-500 border-b border-slate-100">
            <tr>
              <th class="py-2">Obra</th>
              <th class="py-2">Solicitante</th>
              <th class="py-2">Fechas</th>
              <th class="py-2">Estado</th>
            </tr>
          </thead>
          <tbody>
            @for (l of loans(); track l.id) {
              <tr class="border-b border-slate-50">
                <td class="py-3">
                  <p class="font-medium">{{ l.artworkTitle }}</p>
                  <p class="text-xs text-slate-400">{{ l.artworkArtist }}</p>
                </td>
                <td class="py-3">{{ l.foundationName }}</td>
                <td class="py-3 text-slate-600">{{ l.startDate }} → {{ l.endDate }}</td>
                <td class="py-3">
                  <span class="inline-block px-2 py-0.5 rounded bg-slate-100 text-slate-700">
                    {{ statusLabel(l.status) }}
                  </span>
                </td>
              </tr>
            }
          </tbody>
        </table>
      }
    </section>
  `,
})
export class CollectorDashboardComponent implements OnInit {
  private readonly loanService = inject(LoanService);
  private readonly auth = inject(AuthService);

  protected readonly loans = signal<LoanResponse[]>([]);
  protected readonly loading = signal(true);

  ngOnInit(): void {
    const collectorId = this.auth.userId();
    if (collectorId == null) return;

    this.loanService.getByCollector(collectorId).subscribe({
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
