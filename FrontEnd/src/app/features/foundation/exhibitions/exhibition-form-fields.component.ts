import { Component, inject, input } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';

import { buildStopGroup, ExhibitionForm } from './exhibition-form';

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

      <!-- Exposición itinerante: sedes siguientes -->
      <div class="sm:col-span-2 border-t border-border pt-4" formArrayName="stops">
        <div class="flex items-center justify-between gap-3 flex-wrap">
          <div>
            <p class="text-sm font-bold text-text-main">Exposición itinerante</p>
            <p class="text-xs text-text-secondary">
              Si la exposición viaja a otras sedes, añádelas en orden. Las obras irán de una sede a otra sin
              volver al prestador, y el préstamo deberá cubrir la gira completa.
            </p>
          </div>
          @if (form().controls.stops.length < 10) {
            <button type="button" (click)="addStop()"
                    class="inline-flex items-center gap-1 rounded-lg h-9 px-3 border border-border text-sm font-medium hover:bg-gray-soft">
              <span class="material-symbols-outlined" style="font-size:18px;">add_location_alt</span> Añadir sede
            </button>
          }
        </div>
        @for (stop of form().controls.stops.controls; track stop; let i = $index) {
          <div [formGroupName]="i" class="mt-3 rounded-xl border border-border p-4 grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div class="sm:col-span-2 flex items-center justify-between">
              <p class="text-sm font-bold text-text-main">Sede {{ i + 2 }}</p>
              <button type="button" (click)="removeStop(i)" title="Quitar sede"
                      class="text-text-secondary hover:text-red-600">
                <span class="material-symbols-outlined" style="font-size:18px;">delete</span>
              </button>
            </div>
            <label class="block sm:col-span-2">
              <p class="text-xs font-medium text-text-main pb-1">Institución que la acoge</p>
              <input formControlName="hostInstitution" placeholder="Ej. Museo de Bellas Artes de Bilbao"
                     class="w-full rounded-lg border border-border h-10 px-3 text-sm" />
            </label>
            <label class="block">
              <p class="text-xs font-medium text-text-main pb-1">Sala</p>
              <input formControlName="venueName" class="w-full rounded-lg border border-border h-10 px-3 text-sm" />
            </label>
            <div class="grid grid-cols-2 gap-3">
              <label class="block">
                <p class="text-xs font-medium text-text-main pb-1">Ciudad</p>
                <input formControlName="venueCity" class="w-full rounded-lg border border-border h-10 px-3 text-sm" />
              </label>
              <label class="block">
                <p class="text-xs font-medium text-text-main pb-1">País</p>
                <select formControlName="venueCountry" class="w-full rounded-lg border border-border bg-white h-10 px-3 text-sm">
                  @for (c of countries; track c.code) {
                    <option [value]="c.code">{{ c.name }}</option>
                  }
                </select>
              </label>
            </div>
            <label class="block">
              <p class="text-xs font-medium text-text-main pb-1">Apertura</p>
              <input type="date" formControlName="openingDate" class="w-full rounded-lg border border-border h-10 px-3 text-sm" />
            </label>
            <label class="block">
              <p class="text-xs font-medium text-text-main pb-1">Cierre</p>
              <input type="date" formControlName="closingDate" class="w-full rounded-lg border border-border h-10 px-3 text-sm" />
            </label>
          </div>
        }
      </div>
    </div>
  `,
})
export class ExhibitionFormFieldsComponent {
  readonly form = input.required<ExhibitionForm>();
  private readonly fb = inject(FormBuilder);

  addStop(): void {
    this.form().controls.stops.push(buildStopGroup(this.fb, { venueCountry: 'ES' }));
  }

  removeStop(index: number): void {
    this.form().controls.stops.removeAt(index);
  }

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
