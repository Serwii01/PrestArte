import { CommonModule } from '@angular/common';
import { Component, computed, input } from '@angular/core';

import { CompatibilityReport } from '../../../core/models/facility.models';

const AREA_ICON: Record<string, string> = {
  CLIMATE: 'thermostat',
  LIGHT: 'light_mode',
  SECURITY: 'security',
  ACCESS: 'door_front',
  NOTICE: 'event',
  REPORT: 'description',
};

/**
 * Muestra el resultado de comparar los requisitos de una obra con el
 * informe de instalaciones de una fundación: incumplimientos en rojo y
 * avisos (datos que faltan o desactualizados) en ámbar.
 */
@Component({
  selector: 'app-compatibility-panel',
  standalone: true,
  imports: [CommonModule],
  template: `
    @if (report(); as r) {
      <div class="rounded-xl border p-4"
           [class.border-emerald-200]="r.result === 'OK'" [class.bg-emerald-50]="r.result === 'OK'"
           [class.border-amber-200]="r.result === 'WARNINGS'" [class.bg-amber-50]="r.result === 'WARNINGS'"
           [class.border-red-200]="r.result === 'BLOCKERS'" [class.bg-red-50]="r.result === 'BLOCKERS'">
        <div class="flex items-start gap-2">
          <span class="material-symbols-outlined mt-0.5"
                [class.text-emerald-600]="r.result === 'OK'"
                [class.text-amber-600]="r.result === 'WARNINGS'"
                [class.text-red-600]="r.result === 'BLOCKERS'">{{ headerIcon() }}</span>
          <div class="flex-1">
            <p class="text-sm font-bold text-text-main">{{ headerText() }}</p>
            @if (r.facilityReportUpdatedAt) {
              <p class="text-xs text-text-secondary">
                Según el informe de instalaciones del {{ r.facilityReportUpdatedAt | date:'mediumDate' }}.
              </p>
            }
          </div>
        </div>
        @if (r.issues.length > 0) {
          <ul class="mt-3 space-y-2">
            @for (i of r.issues; track $index) {
              <li class="flex items-start gap-2 text-sm">
                <span class="material-symbols-outlined shrink-0 mt-0.5" style="font-size:18px;"
                      [class.text-red-600]="i.severity === 'BLOCKER'"
                      [class.text-amber-600]="i.severity === 'WARNING'">{{ areaIcon(i.area) }}</span>
                <span [class.text-red-800]="i.severity === 'BLOCKER'"
                      [class.text-amber-900]="i.severity === 'WARNING'">{{ i.message }}</span>
              </li>
            }
          </ul>
        }
      </div>
    }
  `,
})
export class CompatibilityPanelComponent {
  readonly report = input<CompatibilityReport | null>(null);
  /** Quién lo lee: cambia el texto de la cabecera. */
  readonly perspective = input<'foundation' | 'collector'>('foundation');

  protected readonly headerIcon = computed(() => {
    switch (this.report()?.result) {
      case 'OK':
        return 'verified';
      case 'BLOCKERS':
        return 'block';
      default:
        return 'info';
    }
  });

  protected readonly headerText = computed(() => {
    const r = this.report();
    if (!r) return '';
    const collector = this.perspective() === 'collector';
    switch (r.result) {
      case 'OK':
        return collector
          ? 'Las instalaciones de la institución cumplen los requisitos de tu obra.'
          : 'Tus instalaciones cumplen los requisitos de esta obra.';
      case 'BLOCKERS':
        return collector
          ? 'La institución declara no cumplir algunos requisitos de tu obra.'
          : 'Tus instalaciones no cumplen algunos requisitos de esta obra. Puedes solicitarla igualmente, pero el coleccionista lo verá.';
      default:
        return 'No hay incumplimientos claros, pero falta información por confirmar.';
    }
  });

  protected areaIcon(area: string): string {
    return AREA_ICON[area] ?? 'info';
  }
}
