import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { environment } from '../../../environments/environment';
import { LOAN_STATUS_LABEL, LoanStatus } from '../../core/models/loan.models';

interface AdminMetrics {
  approvedUsersByRole: Record<string, number>;
  pendingUsers: number;
  deletedUsers: number;
  artworks: number;
  lendableArtworks: number;
  verifiedArtworks: number;
  flaggedArtworks: number;
  pendingDueDiligence: number;
  loansByStatus: Record<string, number>;
  activeLoans: number;
  completedLoans: number;
  acceptanceRate: number | null;
  avgDaysToAnswer: number | null;
  insuredValueOut: number;
  exhibitions: number;
  damageIncidents: number;
  mostRequested: { id: number; title: string; artist: string; requests: number }[];
}

const ROLE_LABEL: Record<string, string> = {
  COLLECTOR: 'Coleccionistas',
  FOUNDATION: 'Fundaciones',
  TRANSPORT: 'Transportistas',
};

/** Indicadores de la red para la administración. */
@Component({
  selector: 'app-admin-metrics',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    @if (m(); as x) {
      <section class="mb-10">
        <h2 class="text-lg font-bold text-text-main mb-3 flex items-center gap-2">
          <span class="material-symbols-outlined text-primary">monitoring</span> Estado de la red
        </h2>

        <div class="grid grid-cols-2 md:grid-cols-4 gap-3">
          @for (k of kpis(); track k.label) {
            <div class="stitch-card p-4">
              <p class="text-xs uppercase font-bold tracking-wider text-text-secondary">{{ k.label }}</p>
              <p class="text-2xl font-black text-text-main mt-1">{{ k.value }}</p>
              @if (k.hint) { <p class="text-xs text-text-secondary mt-0.5">{{ k.hint }}</p> }
            </div>
          }
        </div>

        <div class="grid grid-cols-1 lg:grid-cols-3 gap-4 mt-4">
          <div class="stitch-card p-5">
            <p class="text-sm font-bold text-text-main mb-3">Usuarios verificados</p>
            <ul class="space-y-1.5 text-sm">
              @for (r of roles(); track r.label) {
                <li class="flex justify-between"><span>{{ r.label }}</span><span class="font-bold">{{ r.value }}</span></li>
              }
              <li class="flex justify-between text-text-secondary"><span>Pendientes de aprobar</span><span>{{ x.pendingUsers }}</span></li>
              <li class="flex justify-between text-text-secondary"><span>Bajas</span><span>{{ x.deletedUsers }}</span></li>
            </ul>
          </div>

          <div class="stitch-card p-5">
            <p class="text-sm font-bold text-text-main mb-3">Préstamos por estado</p>
            <ul class="space-y-1.5 text-sm">
              @for (s of statuses(); track s.label) {
                <li>
                  <div class="flex justify-between"><span>{{ s.label }}</span><span class="font-bold">{{ s.value }}</span></div>
                  <div class="h-1.5 rounded-full bg-gray-soft mt-1">
                    <div class="h-1.5 rounded-full bg-primary" [style.width.%]="s.pct"></div>
                  </div>
                </li>
              } @empty {
                <li class="text-text-secondary">Todavía no hay préstamos.</li>
              }
            </ul>
          </div>

          <div class="stitch-card p-5">
            <p class="text-sm font-bold text-text-main mb-3">Obras más solicitadas</p>
            <ol class="space-y-2 text-sm">
              @for (a of x.mostRequested; track a.id; let i = $index) {
                <li class="flex justify-between gap-2">
                  <a [routerLink]="['/app/artworks', a.id]" class="truncate hover:text-primary">
                    {{ i + 1 }}. {{ a.title }} <span class="text-text-secondary">· {{ a.artist }}</span>
                  </a>
                  <span class="font-bold shrink-0">{{ a.requests }}</span>
                </li>
              } @empty {
                <li class="text-text-secondary">Sin solicitudes todavía.</li>
              }
            </ol>
          </div>
        </div>
      </section>
    }
  `,
})
export class AdminMetricsComponent implements OnInit {
  private readonly http = inject(HttpClient);
  protected readonly m = signal<AdminMetrics | null>(null);

  protected readonly kpis = computed(() => {
    const x = this.m();
    if (!x) return [];
    const eur = new Intl.NumberFormat('es-ES', { style: 'currency', currency: 'EUR', maximumFractionDigits: 0 });
    return [
      { label: 'Préstamos activos', value: x.activeLoans, hint: `${x.completedLoans} completados` },
      { label: 'Tasa de aceptación', value: x.acceptanceRate != null ? `${x.acceptanceRate} %` : '—', hint: 'de las solicitudes respondidas' },
      { label: 'Tiempo de respuesta', value: x.avgDaysToAnswer != null ? `${x.avgDaysToAnswer} días` : '—', hint: 'media del coleccionista' },
      { label: 'Valor fuera de casa', value: eur.format(x.insuredValueOut), hint: 'asegurado, en tránsito o expuesto' },
      { label: 'Obras', value: x.artworks, hint: `${x.lendableArtworks} disponibles` },
      { label: 'Procedencia verificada', value: x.verifiedArtworks, hint: `${x.pendingDueDiligence} pendientes · ${x.flaggedArtworks} retiradas` },
      { label: 'Exposiciones', value: x.exhibitions },
      { label: 'Incidencias', value: x.damageIncidents, hint: 'informes de estado con daños' },
    ];
  });

  protected readonly roles = computed(() =>
    Object.entries(this.m()?.approvedUsersByRole ?? {}).map(([k, v]) => ({ label: ROLE_LABEL[k] ?? k, value: v })),
  );

  protected readonly statuses = computed(() => {
    const entries = Object.entries(this.m()?.loansByStatus ?? {});
    const max = Math.max(1, ...entries.map(([, v]) => v));
    return entries
      .sort((a, b) => b[1] - a[1])
      .map(([k, v]) => ({ label: LOAN_STATUS_LABEL[k as LoanStatus] ?? k, value: v, pct: (v / max) * 100 }));
  });

  ngOnInit(): void {
    this.http.get<AdminMetrics>(`${environment.apiBaseUrl}/admin/metrics`).subscribe({
      next: (x) => this.m.set(x),
      error: () => this.m.set(null),
    });
  }
}
