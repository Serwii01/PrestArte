import { CommonModule } from '@angular/common';
import { Component, inject, input, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { ProvenanceService } from '../../core/services/provenance.service';
import {
  ACQUISITION_LABEL,
  AcquisitionMethod,
  ProvenanceEntry,
  ProvenanceResponse,
} from '../../core/models/provenance.models';
import { SecureFilePipe } from '../../shared/pipes/secure-file.pipe';

/**
 * Titularidad y procedencia de una obra: declaración del coleccionista,
 * historial de propietarios, huecos (con aviso especial en 1933–1945) y
 * resultado de la diligencia debida. El dueño puede añadir y borrar eslabones.
 */
@Component({
  selector: 'app-provenance-section',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, SecureFilePipe],
  template: `
    @if (data(); as p) {
      <section class="stitch-card p-5">
        <div class="flex items-start justify-between gap-3 flex-wrap mb-3">
          <h3 class="text-sm font-bold text-text-main flex items-center gap-2">
            <span class="material-symbols-outlined text-primary" style="font-size:18px;">history_edu</span>
            Titularidad y procedencia
          </h3>
          <div class="flex flex-wrap gap-1.5">
            <span class="px-2 py-0.5 rounded-full text-[11px] font-bold"
                  [class.bg-emerald-100]="p.titleDeclaredAt" [class.text-emerald-800]="p.titleDeclaredAt"
                  [class.bg-amber-100]="!p.titleDeclaredAt" [class.text-amber-800]="!p.titleDeclaredAt">
              {{ p.titleDeclaredAt ? 'Titularidad declarada' : 'Titularidad sin declarar' }}
            </span>
            <span class="px-2 py-0.5 rounded-full text-[11px] font-bold"
                  [class.bg-emerald-100]="p.dueDiligenceStatus === 'CLEAR'" [class.text-emerald-800]="p.dueDiligenceStatus === 'CLEAR'"
                  [class.bg-gray-soft]="p.dueDiligenceStatus === 'PENDING'" [class.text-text-secondary]="p.dueDiligenceStatus === 'PENDING'"
                  [class.bg-red-100]="p.dueDiligenceStatus === 'FLAGGED'" [class.text-red-800]="p.dueDiligenceStatus === 'FLAGGED'">
              {{ p.dueDiligenceStatus === 'CLEAR' ? 'Procedencia verificada'
                : p.dueDiligenceStatus === 'FLAGGED' ? 'Retirada para revisión' : 'Pendiente de verificación' }}
            </span>
          </div>
        </div>

        @if (!p.titleDeclaredAt && p.editable) {
          <div class="mb-4 p-3 rounded-lg bg-amber-50 border border-amber-200">
            <p class="text-xs text-amber-900 leading-relaxed">{{ p.titleDeclarationText }}</p>
            <button type="button" (click)="declare()" [disabled]="busy()"
                    class="mt-2 inline-flex items-center gap-1 rounded-lg h-9 px-3 bg-primary hover:bg-primary-hover text-white text-xs font-bold disabled:opacity-50">
              <span class="material-symbols-outlined" style="font-size:16px;">draw</span> Firmar la declaración
            </button>
          </div>
        }

        @if (p.entries.length === 0) {
          <p class="text-sm text-text-secondary">Todavía no hay historial de procedencia.</p>
        } @else {
          <ol class="relative border-l border-border ml-2 space-y-4">
            @for (e of p.entries; track e.id) {
              <li class="ml-5">
                <span class="absolute -left-[7px] mt-1.5 size-3 rounded-full bg-primary border-2 border-white"></span>
                <div class="flex items-start justify-between gap-2">
                  <div>
                    <p class="text-sm font-bold text-text-main">{{ e.owner }}</p>
                    <p class="text-xs text-text-secondary">
                      {{ e.fromYear }}–{{ e.toYear ?? 'actualidad' }} · {{ acquisitionLabel[e.acquisitionMethod] }}
                    </p>
                  </div>
                  @if (p.editable) {
                    <button type="button" (click)="remove(e)" [disabled]="busy()" title="Eliminar"
                            class="text-text-secondary hover:text-red-600 disabled:opacity-50">
                      <span class="material-symbols-outlined" style="font-size:18px;">delete</span>
                    </button>
                  }
                </div>
                @if (e.notes) { <p class="text-xs text-text-main mt-1 whitespace-pre-line">{{ e.notes }}</p> }
                @if (e.documentFileId) {
                  <a [href]="e.documentFileId | secureFile | async" [download]="e.documentFileName"
                     class="mt-1 inline-flex items-center gap-1 text-xs font-bold text-primary hover:underline">
                    <span class="material-symbols-outlined" style="font-size:14px;">attach_file</span> {{ e.documentFileName }}
                  </a>
                } @else if (e.hasDocument) {
                  <p class="mt-1 text-[11px] text-text-secondary flex items-center gap-1">
                    <span class="material-symbols-outlined" style="font-size:14px;">lock</span>
                    Justificante disponible para las instituciones que soliciten la obra.
                  </p>
                }
              </li>
            }
          </ol>
        }

        @if (p.gaps.length > 0) {
          <ul class="mt-4 space-y-1.5">
            @for (g of p.gaps; track $index) {
              <li class="text-xs flex items-start gap-1.5"
                  [class.text-red-800]="g.critical" [class.text-amber-900]="!g.critical">
                <span class="material-symbols-outlined shrink-0" style="font-size:16px;">{{ g.critical ? 'report' : 'warning' }}</span>
                {{ g.message }}
              </li>
            }
          </ul>
        }

        @if (p.dueDiligence; as d) {
          <p class="mt-4 text-[11px] text-text-secondary">
            Revisada por {{ d.checkedByName }} el {{ d.checkedAt | date:'mediumDate' }} · Fuentes: {{ d.sources }}
            @if (d.reference) { · Ref. {{ d.reference }} }
          </p>
          @if (d.notes) { <p class="text-[11px] text-text-secondary">Notas internas: {{ d.notes }}</p> }
        }

        @if (p.editable) {
          @if (!showForm()) {
            <button type="button" (click)="openForm()"
                    class="mt-4 inline-flex items-center gap-1 text-xs font-bold text-primary hover:underline">
              <span class="material-symbols-outlined" style="font-size:16px;">add</span> Añadir propietario anterior
            </button>
          } @else {
            <form [formGroup]="form" (ngSubmit)="add()" class="mt-4 p-4 rounded-lg border border-border space-y-3">
              <label class="block">
                <p class="text-xs font-medium text-text-secondary pb-1">Propietario o poseedor</p>
                <input formControlName="owner" placeholder="Ej. Galería X, Madrid · Colección particular, París"
                       class="w-full rounded-lg border border-border h-10 px-3 text-sm" />
              </label>
              <div class="grid grid-cols-3 gap-2">
                <label class="block">
                  <p class="text-xs font-medium text-text-secondary pb-1">Desde (año)</p>
                  <input type="number" formControlName="fromYear" class="w-full rounded-lg border border-border h-10 px-3 text-sm" />
                </label>
                <label class="block">
                  <p class="text-xs font-medium text-text-secondary pb-1">Hasta (vacío = hoy)</p>
                  <input type="number" formControlName="toYear" class="w-full rounded-lg border border-border h-10 px-3 text-sm" />
                </label>
                <label class="block">
                  <p class="text-xs font-medium text-text-secondary pb-1">Cómo la adquirió</p>
                  <select formControlName="acquisitionMethod" class="w-full rounded-lg border border-border bg-white h-10 px-2 text-sm">
                    @for (m of methods; track m) { <option [value]="m">{{ acquisitionLabel[m] }}</option> }
                  </select>
                </label>
              </div>
              <label class="block">
                <p class="text-xs font-medium text-text-secondary pb-1">Detalles (subasta y lote, galería, referencias…)</p>
                <textarea rows="2" formControlName="notes" class="w-full rounded-lg border border-border px-3 py-2 text-sm"></textarea>
              </label>
              <label class="block">
                <p class="text-xs font-medium text-text-secondary pb-1">Justificante (opcional: factura, catálogo, certificado…)</p>
                <input type="file" accept="application/pdf,image/jpeg,image/png,image/webp" (change)="onFile($event)" class="text-xs" />
              </label>
              @if (error()) { <p class="text-xs text-red-700">{{ error() }}</p> }
              <div class="flex justify-end gap-2">
                <button type="button" (click)="showForm.set(false)"
                        class="px-3 py-1.5 rounded-lg bg-white border border-border text-xs font-medium">Cancelar</button>
                <button type="submit" [disabled]="form.invalid || busy()"
                        class="px-3 py-1.5 rounded-lg bg-primary hover:bg-primary-hover text-white text-xs font-bold disabled:opacity-50">Añadir</button>
              </div>
            </form>
          }
        }
      </section>
    }
  `,
})
export class ProvenanceSectionComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly service = inject(ProvenanceService);

  readonly artworkId = input.required<number>();

  protected readonly data = signal<ProvenanceResponse | null>(null);
  protected readonly busy = signal(false);
  protected readonly showForm = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly file = signal<File | null>(null);
  protected readonly acquisitionLabel = ACQUISITION_LABEL;
  protected readonly methods = Object.keys(ACQUISITION_LABEL) as AcquisitionMethod[];

  protected readonly form = this.fb.nonNullable.group({
    owner: ['', [Validators.required, Validators.maxLength(300)]],
    fromYear: [null as number | null, [Validators.required, Validators.min(0)]],
    toYear: [null as number | null, [Validators.min(0)]],
    acquisitionMethod: ['PURCHASE' as AcquisitionMethod, [Validators.required]],
    notes: ['', [Validators.maxLength(2000)]],
  });

  ngOnInit(): void {
    this.service.get(this.artworkId()).subscribe({
      next: (p) => this.data.set(p),
      // Sin sesión o sin acceso, la sección simplemente no se muestra.
      error: () => this.data.set(null),
    });
  }

  declare(): void {
    this.busy.set(true);
    this.service.declareTitle(this.artworkId()).subscribe({
      next: (p) => this.done(p),
      error: (err) => this.fail(err),
    });
  }

  openForm(): void {
    this.form.reset({ owner: '', fromYear: null, toYear: null, acquisitionMethod: 'PURCHASE', notes: '' });
    this.file.set(null);
    this.error.set(null);
    this.showForm.set(true);
  }

  onFile(event: Event): void {
    this.file.set((event.target as HTMLInputElement).files?.[0] ?? null);
  }

  add(): void {
    if (this.form.invalid) return;
    const v = this.form.getRawValue();
    this.busy.set(true);
    this.service
      .addEntry(this.artworkId(), {
        owner: v.owner.trim(),
        fromYear: v.fromYear!,
        toYear: v.toYear,
        acquisitionMethod: v.acquisitionMethod,
        notes: v.notes.trim() || undefined,
      }, this.file())
      .subscribe({
        next: (p) => {
          this.done(p);
          this.showForm.set(false);
        },
        error: (err) => this.fail(err),
      });
  }

  remove(e: ProvenanceEntry): void {
    if (!confirm(`¿Eliminar «${e.owner}» del historial?`)) return;
    this.busy.set(true);
    this.service.deleteEntry(this.artworkId(), e.id).subscribe({
      next: (p) => this.done(p),
      error: (err) => this.fail(err),
    });
  }

  private done(p: ProvenanceResponse): void {
    this.data.set(p);
    this.busy.set(false);
    this.error.set(null);
  }

  private fail(err: { error?: { message?: string } }): void {
    this.busy.set(false);
    this.error.set(err?.error?.message ?? 'No se pudo completar la acción.');
  }
}
