import { CommonModule } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { ProvenanceService } from '../../core/services/provenance.service';
import { DueDiligencePendingItem } from '../../core/models/provenance.models';

/**
 * Cola de obras pendientes de diligencia debida. La administración las
 * comprueba en bases de datos de obras robadas o expoliadas y anota el
 * resultado: verificada o retirada.
 */
@Component({
  selector: 'app-due-diligence-panel',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
    <section class="stitch-card overflow-hidden mb-10">
      <div class="p-5 border-b border-border">
        <h2 class="text-lg font-bold text-text-main flex items-center gap-2">
          <span class="material-symbols-outlined text-primary">policy</span>
          Diligencia debida de obras
        </h2>
        <p class="text-sm text-text-secondary mt-1">
          Comprueba cada obra en bases de obras robadas o expoliadas (Interpol ID-Art, Art Loss Register,
          German Lost Art…) antes de darla por verificada. Si hay indicios, márcala: saldrá del catálogo.
        </p>
      </div>

      @if (loading()) {
        <p class="p-6 text-sm text-text-secondary">Cargando…</p>
      } @else if (items().length === 0) {
        <p class="p-6 text-sm text-text-secondary">No hay obras pendientes de revisar.</p>
      } @else {
        <ul class="divide-y divide-border">
          @for (i of items(); track i.artworkId) {
            <li class="p-4">
              <div class="flex items-start justify-between gap-3 flex-wrap">
                <div class="min-w-0">
                  <a [routerLink]="['/app/artworks', i.artworkId]" class="text-sm font-bold text-text-main hover:text-primary">
                    {{ i.title }}
                  </a>
                  <p class="text-xs text-text-secondary">
                    {{ i.artist }}{{ i.year ? ' · ' + i.year : '' }} · {{ i.collectorName }}
                  </p>
                  <div class="mt-1 flex flex-wrap gap-1.5 text-[11px] font-bold">
                    <span class="px-2 py-0.5 rounded-full"
                          [class.bg-emerald-100]="i.titleDeclaredAt" [class.text-emerald-800]="i.titleDeclaredAt"
                          [class.bg-amber-100]="!i.titleDeclaredAt" [class.text-amber-800]="!i.titleDeclaredAt">
                      {{ i.titleDeclaredAt ? 'Titularidad declarada' : 'Sin declaración' }}
                    </span>
                    <span class="px-2 py-0.5 rounded-full bg-gray-soft text-text-main">{{ i.provenanceEntries }} propietarios</span>
                    @if (i.provenanceGaps > 0) {
                      <span class="px-2 py-0.5 rounded-full"
                            [class.bg-red-100]="i.criticalGap" [class.text-red-800]="i.criticalGap"
                            [class.bg-amber-100]="!i.criticalGap" [class.text-amber-800]="!i.criticalGap">
                        {{ i.provenanceGaps }} huecos{{ i.criticalGap ? ' (1933–1945)' : '' }}
                      </span>
                    }
                  </div>
                </div>
                @if (openId() !== i.artworkId) {
                  <button type="button" (click)="open(i)"
                          class="inline-flex items-center gap-1 rounded-lg h-9 px-3 border border-border text-xs font-bold hover:bg-gray-soft">
                    <span class="material-symbols-outlined" style="font-size:16px;">fact_check</span> Registrar revisión
                  </button>
                }
              </div>

              @if (openId() === i.artworkId) {
                <form [formGroup]="form" (ngSubmit)="save(i)" class="mt-3 p-4 rounded-lg border border-border space-y-3">
                  <label class="block">
                    <p class="text-xs font-medium text-text-secondary pb-1">Bases consultadas</p>
                    <input formControlName="sources" class="w-full rounded-lg border border-border h-10 px-3 text-sm" />
                  </label>
                  <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
                    <label class="block">
                      <p class="text-xs font-medium text-text-secondary pb-1">Referencia (certificado, búsqueda…)</p>
                      <input formControlName="reference" class="w-full rounded-lg border border-border h-10 px-3 text-sm" />
                    </label>
                    <label class="block">
                      <p class="text-xs font-medium text-text-secondary pb-1">Resultado</p>
                      <select formControlName="result" class="w-full rounded-lg border border-border bg-white h-10 px-2 text-sm">
                        <option value="CLEAR">Sin incidencias: procedencia verificada</option>
                        <option value="FLAGGED">Indicios de problema: retirar del catálogo</option>
                      </select>
                    </label>
                  </div>
                  <label class="block">
                    <p class="text-xs font-medium text-text-secondary pb-1">Notas internas</p>
                    <textarea rows="2" formControlName="notes" class="w-full rounded-lg border border-border px-3 py-2 text-sm"></textarea>
                  </label>
                  @if (error()) { <p class="text-xs text-red-700">{{ error() }}</p> }
                  <div class="flex justify-end gap-2">
                    <button type="button" (click)="openId.set(null)"
                            class="px-3 py-1.5 rounded-lg bg-white border border-border text-xs font-medium">Cancelar</button>
                    <button type="submit" [disabled]="form.invalid || saving()"
                            class="px-3 py-1.5 rounded-lg text-white text-xs font-bold disabled:opacity-50"
                            [class.bg-emerald-600]="form.controls.result.value === 'CLEAR'"
                            [class.bg-red-600]="form.controls.result.value === 'FLAGGED'">Guardar revisión</button>
                  </div>
                </form>
              }
            </li>
          }
        </ul>
      }
    </section>
  `,
})
export class DueDiligencePanelComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly service = inject(ProvenanceService);

  protected readonly items = signal<DueDiligencePendingItem[]>([]);
  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected readonly openId = signal<number | null>(null);
  protected readonly error = signal<string | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    result: ['CLEAR' as 'CLEAR' | 'FLAGGED', [Validators.required]],
    sources: ['Interpol ID-Art, Art Loss Register', [Validators.required, Validators.maxLength(300)]],
    reference: ['', [Validators.maxLength(100)]],
    notes: ['', [Validators.maxLength(5000)]],
  });

  ngOnInit(): void {
    this.load();
  }

  open(i: DueDiligencePendingItem): void {
    this.form.reset({ result: 'CLEAR', sources: 'Interpol ID-Art, Art Loss Register', reference: '', notes: '' });
    this.error.set(null);
    this.openId.set(i.artworkId);
  }

  save(i: DueDiligencePendingItem): void {
    if (this.form.invalid) return;
    const v = this.form.getRawValue();
    if (v.result === 'FLAGGED' && !confirm(`«${i.title}» saldrá del catálogo. ¿Continuar?`)) return;
    this.saving.set(true);
    this.service
      .recordDueDiligence(i.artworkId, {
        result: v.result,
        sources: v.sources.trim(),
        reference: v.reference.trim() || undefined,
        notes: v.notes.trim() || undefined,
      })
      .subscribe({
        next: () => {
          this.saving.set(false);
          this.openId.set(null);
          this.load();
        },
        error: (err) => {
          this.saving.set(false);
          this.error.set(err?.error?.message ?? 'No se pudo guardar la revisión.');
        },
      });
  }

  private load(): void {
    this.loading.set(true);
    this.service.pending().subscribe({
      next: (list) => {
        this.items.set(list);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }
}
