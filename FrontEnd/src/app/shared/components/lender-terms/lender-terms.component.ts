import { CommonModule } from '@angular/common';
import { Component, computed, input } from '@angular/core';

import { ArtworkResponse, IMAGE_RIGHTS_LABEL, ImageRights } from '../../../core/models/artwork.models';

/** Condiciones del prestador: mención en cartela, usos de imagen y canon. */
@Component({
  selector: 'app-lender-terms',
  standalone: true,
  imports: [CommonModule],
  template: `
    @if (artwork(); as a) {
      <dl class="grid grid-cols-1 sm:grid-cols-2 gap-3 text-sm">
        <div>
          <dt class="text-xs uppercase font-bold tracking-wider text-text-secondary">Mención en cartela</dt>
          <dd class="mt-1 font-medium text-text-main">{{ a.creditLine || 'Colección particular' }}</dd>
        </div>
        <div>
          <dt class="text-xs uppercase font-bold tracking-wider text-text-secondary">Canon de préstamo</dt>
          <dd class="mt-1 font-medium text-text-main">
            {{ a.loanFee ? (a.loanFee | number:'1.0-0') + ' €' : 'Gratuito' }}
          </dd>
        </div>
        <div class="sm:col-span-2">
          <dt class="text-xs uppercase font-bold tracking-wider text-text-secondary">Usos de la imagen</dt>
          <dd class="mt-1 flex flex-wrap gap-1.5">
            @for (r of rights(); track r.key) {
              <span class="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[11px] font-bold"
                    [class.bg-emerald-100]="r.allowed" [class.text-emerald-800]="r.allowed"
                    [class.bg-gray-soft]="!r.allowed" [class.text-text-secondary]="!r.allowed">
                <span class="material-symbols-outlined" style="font-size:12px;">{{ r.allowed ? 'check' : 'block' }}</span>
                {{ r.label }}
              </span>
            }
          </dd>
        </div>
      </dl>
    }
  `,
})
export class LenderTermsComponent {
  readonly artwork = input<ArtworkResponse | null>(null);

  protected readonly rights = computed(() => {
    const r: ImageRights = this.artwork()?.imageRights ?? {};
    return (Object.keys(IMAGE_RIGHTS_LABEL) as (keyof ImageRights)[]).map((key) => ({
      key,
      label: IMAGE_RIGHTS_LABEL[key],
      allowed: !!r[key],
    }));
  });
}
