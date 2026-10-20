import { CommonModule } from '@angular/common';
import { Component, computed, input } from '@angular/core';

import { LoanRequirements } from '../../../core/models/artwork.models';

/** Lista legible de los requisitos de préstamo de una obra. */
@Component({
  selector: 'app-requirements-summary',
  standalone: true,
  imports: [CommonModule],
  template: `
    @if (items().length > 0) {
      <ul class="grid grid-cols-1 sm:grid-cols-2 gap-2">
        @for (it of items(); track it.label) {
          <li class="flex items-start gap-2 text-sm text-text-main">
            <span class="material-symbols-outlined text-primary shrink-0" style="font-size:18px;">{{ it.icon }}</span>
            <span>{{ it.label }}</span>
          </li>
        }
      </ul>
    } @else {
      <p class="text-sm text-text-secondary">El coleccionista no ha indicado requisitos específicos.</p>
    }
  `,
})
export class RequirementsSummaryComponent {
  readonly requirements = input<LoanRequirements | null | undefined>(null);

  protected readonly items = computed(() => {
    const r = this.requirements();
    if (!r) return [];
    const out: { icon: string; label: string }[] = [];
    const range = (min?: number | null, max?: number | null, unit = '') =>
      min != null && max != null ? `${min}–${max}${unit}` : min != null ? `≥ ${min}${unit}` : `≤ ${max}${unit}`;
    if (r.tempMin != null || r.tempMax != null) {
      out.push({ icon: 'thermostat', label: `Temperatura ${range(r.tempMin, r.tempMax, ' °C')}` });
    }
    if (r.rhMin != null || r.rhMax != null) {
      out.push({ icon: 'water_drop', label: `Humedad relativa ${range(r.rhMin, r.rhMax, ' %')}` });
    }
    if (r.maxLux != null) out.push({ icon: 'light_mode', label: `Máximo ${r.maxLux} lux` });
    if (r.climateControl) out.push({ icon: 'ac_unit', label: 'Climatización controlada' });
    if (r.displayCase) out.push({ icon: 'view_in_ar', label: 'Exposición en vitrina' });
    if (r.guards24h) out.push({ icon: 'person_alert', label: 'Vigilancia 24 h' });
    if (r.cctv) out.push({ icon: 'videocam', label: 'Videovigilancia (CCTV)' });
    if (r.intrusionAlarm) out.push({ icon: 'notifications_active', label: 'Alarma de intrusión' });
    if (r.fireDetection) out.push({ icon: 'local_fire_department', label: 'Detección de incendios' });
    if (r.courierMode === 'VIRTUAL') {
      out.push({ icon: 'videocam', label: 'Correo virtual: el prestador aprueba el embalaje con fotos' });
    } else if (r.courierMode === 'IN_PERSON') {
      out.push({ icon: 'person_pin_circle', label: 'Correo presencial acompañando la obra' });
    }
    if (r.minNoticeDays != null) {
      out.push({ icon: 'event', label: `Solicitar con ${r.minNoticeDays} días de antelación` });
    }
    return out;
  });
}
