import { CommonModule } from '@angular/common';
import { Component, input, signal } from '@angular/core';

import { CostEstimate } from '../../../core/models/cost.models';

/** Coste y huella de carbono estimados de un préstamo, con sus supuestos. */
@Component({
  selector: 'app-cost-estimate-panel',
  standalone: true,
  imports: [CommonModule],
  template: `
    @if (estimate(); as e) {
      @if (!e.available) {
        <p class="text-sm text-text-secondary flex items-start gap-2">
          <span class="material-symbols-outlined text-text-secondary" style="font-size:18px;">info</span>
          {{ e.message }}
        </p>
      } @else {
        <p class="text-xs text-text-secondary mb-3">
          {{ e.origin }} → {{ e.destination }} · {{ e.distanceKm | number:'1.0-0' }} km
          {{ e.transportMode === 'AIR' ? '(avión)' : 'por carretera' }}
          @if (e.tariffSource) { · tarifa de {{ e.tariffSource }} } @else { · tarifa de referencia }
        </p>
        <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div>
            <p class="text-xs uppercase font-bold tracking-wider text-text-secondary mb-2">Coste estimado</p>
            <ul class="space-y-1.5 text-sm">
              @for (l of e.costLines ?? []; track l.concept) {
                <li class="flex justify-between gap-3">
                  <span class="text-text-main" [title]="l.note ?? ''">{{ l.concept }}</span>
                  <span class="font-medium text-text-main whitespace-nowrap">
                    {{ l.amount == null ? 'A presupuestar' : (l.amount | number:'1.0-0') + ' €' }}
                  </span>
                </li>
              }
            </ul>
            <p class="flex justify-between gap-3 mt-2 pt-2 border-t border-border text-sm font-bold text-text-main">
              <span>Total orientativo</span><span>{{ e.totalCost | number:'1.0-0' }} €</span>
            </p>
          </div>
          <div>
            <p class="text-xs uppercase font-bold tracking-wider text-text-secondary mb-2">Huella de carbono</p>
            <ul class="space-y-1.5 text-sm">
              @for (l of e.co2Lines ?? []; track l.concept) {
                <li class="flex justify-between gap-3">
                  <span class="text-text-main" [title]="l.note ?? ''">{{ l.concept }}</span>
                  <span class="font-medium text-text-main whitespace-nowrap">{{ l.amount | number:'1.0-0' }} kg CO₂e</span>
                </li>
              }
            </ul>
            <p class="flex justify-between gap-3 mt-2 pt-2 border-t border-border text-sm font-bold text-text-main">
              <span>Total</span><span>{{ e.totalCo2Kg | number:'1.0-0' }} kg CO₂e</span>
            </p>
            @if (e.co2SavedWithVirtualCourier) {
              <p class="mt-2 text-xs text-emerald-800 bg-emerald-50 rounded-lg p-2">
                Con correo virtual en lugar de presencial se evitarían unos
                {{ e.co2SavedWithVirtualCourier | number:'1.0-0' }} kg CO₂e y los gastos de viaje.
              </p>
            }
          </div>
        </div>
        <button type="button" (click)="showAssumptions.set(!showAssumptions())"
                class="mt-3 text-xs font-bold text-primary hover:underline">
          {{ showAssumptions() ? 'Ocultar supuestos' : '¿Cómo se calcula?' }}
        </button>
        @if (showAssumptions()) {
          <ul class="mt-2 list-disc pl-5 space-y-1 text-xs text-text-secondary">
            @for (a of e.assumptions ?? []; track a) { <li>{{ a }}</li> }
            <li>Es una estimación para planificar, no un presupuesto. El precio real lo fija el transportista.</li>
          </ul>
        }
      }
    }
  `,
})
export class CostEstimatePanelComponent {
  readonly estimate = input<CostEstimate | null>(null);
  protected readonly showAssumptions = signal(false);
}
