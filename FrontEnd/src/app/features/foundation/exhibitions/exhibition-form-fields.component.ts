import { Component, input } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';

import { ExhibitionForm } from './exhibition-form';

/** Campos del formulario de exposición (se reutilizan en alta y edición). */
@Component({
  selector: 'app-exhibition-form-fields',
  standalone: true,
  imports: [ReactiveFormsModule],
  template: `
    <div [formGroup]="form()" class="grid grid-cols-1 sm:grid-cols-2 gap-4">
      <label class="block sm:col-span-2">
        <p class="text-sm font-medium text-text-main pb-1">Título de la exposición</p>
        <input formControlName="title" placeholder="Ej. Sorolla y el mar"
               class="w-full rounded-lg border border-border h-11 px-3 text-sm" />
      </label>
      <label class="block">
        <p class="text-sm font-medium text-text-main pb-1">Sala o sede</p>
        <input formControlName="venueName" placeholder="Ej. Sala de exposiciones temporales"
               class="w-full rounded-lg border border-border h-11 px-3 text-sm" />
      </label>
      <div class="grid grid-cols-2 gap-3">
        <label class="block">
          <p class="text-sm font-medium text-text-main pb-1">Ciudad</p>
          <input formControlName="venueCity" placeholder="Ej. Valencia"
                 class="w-full rounded-lg border border-border h-11 px-3 text-sm" />
        </label>
        <label class="block">
          <p class="text-sm font-medium text-text-main pb-1">País</p>
          <select formControlName="venueCountry" class="w-full rounded-lg border border-border bg-white h-11 px-3 text-sm">
            @for (c of countries; track c.code) {
              <option [value]="c.code">{{ c.name }}</option>
            }
          </select>
        </label>
      </div>
      @if (form().controls.venueCountry.value !== 'ES') {
        <p class="sm:col-span-2 text-xs text-amber-800 bg-amber-50 border border-amber-200 rounded-lg p-2">
          Fuera de España, las obras con 100 años o más, BIC o inventariadas necesitarán permiso de
          exportación temporal del Ministerio de Cultura. Calcula varios meses de tramitación.
        </p>
      }
      <label class="block">
        <p class="text-sm font-medium text-text-main pb-1">Apertura al público</p>
        <input type="date" formControlName="openingDate"
               class="w-full rounded-lg border border-border h-11 px-3 text-sm" />
      </label>
      <label class="block">
        <p class="text-sm font-medium text-text-main pb-1">Cierre</p>
        <input type="date" formControlName="closingDate"
               class="w-full rounded-lg border border-border h-11 px-3 text-sm" />
      </label>
      <label class="block sm:col-span-2">
        <p class="text-sm font-medium text-text-main pb-1">
          Proyecto curatorial <span class="text-text-secondary font-normal">(lo leerán los coleccionistas)</span>
        </p>
        <textarea rows="4" formControlName="description"
                  placeholder="Qué cuenta la exposición y por qué estas obras son importantes para ella."
                  class="w-full rounded-lg border border-border px-3 py-2 text-sm"></textarea>
      </label>
    </div>
  `,
})
export class ExhibitionFormFieldsComponent {
  readonly form = input.required<ExhibitionForm>();

  /** Países habituales en préstamos desde España; "XX" = cualquier otro. */
  protected readonly countries = [
    { code: 'ES', name: 'España' },
    { code: 'PT', name: 'Portugal' },
    { code: 'FR', name: 'Francia' },
    { code: 'IT', name: 'Italia' },
    { code: 'DE', name: 'Alemania' },
    { code: 'GB', name: 'Reino Unido' },
    { code: 'NL', name: 'Países Bajos' },
    { code: 'BE', name: 'Bélgica' },
    { code: 'CH', name: 'Suiza' },
    { code: 'AT', name: 'Austria' },
    { code: 'US', name: 'Estados Unidos' },
    { code: 'MX', name: 'México' },
    { code: 'XX', name: 'Otro país' },
  ];
}
