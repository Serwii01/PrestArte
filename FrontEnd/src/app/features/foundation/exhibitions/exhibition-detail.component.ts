import { CommonModule } from '@angular/common';
import { Component, inject, Input, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { ExhibitionService } from '../../../core/services/exhibition.service';
import { ExhibitionCompatibility, ExhibitionResponse } from '../../../core/models/exhibition.models';
import { StatusPillComponent } from '../../../shared/components/status-pill/status-pill.component';
import { buildExhibitionForm, exhibitionFormValue, patchExhibitionForm } from './exhibition-form';
import { ExhibitionFormFieldsComponent } from './exhibition-form-fields.component';
import { CompatibilityPanelComponent } from '../../../shared/components/compatibility-panel/compatibility-panel.component';

/** Ficha de una exposición: datos editables y estado de cada obra solicitada. */
@Component({
  selector: 'app-exhibition-detail',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, StatusPillComponent, ExhibitionFormFieldsComponent, CompatibilityPanelComponent],
  template: `
    <div class="mb-6">
      <a routerLink="/app/foundation/exhibitions"
         class="inline-flex items-center gap-1 text-sm font-medium text-text-secondary hover:text-primary transition-colors">
        <span class="material-symbols-outlined" style="font-size:18px;">arrow_back</span>
        Exposiciones
      </a>
    </div>

    @if (loading()) {
      <div class="stitch-card p-12 text-center text-text-secondary">Cargando…</div>
    } @else if (exhibition(); as e) {
      @if (!editing()) {
        <header class="mb-6 flex items-start justify-between gap-4 flex-wrap">
          <div>
            <h1 class="text-3xl md:text-4xl font-black text-text-main tracking-tight">{{ e.title }}</h1>
            <p class="text-text-secondary mt-1">
              {{ e.venueName }} · {{ e.venueCity }} ·
              {{ e.openingDate | date:'mediumDate' }} → {{ e.closingDate | date:'mediumDate' }}
            </p>
            @if (e.stops.length > 0) {
              <div class="mt-3">
                <p class="text-xs font-bold uppercase tracking-wide text-text-secondary">Gira · {{ e.stops.length + 1 }} sedes</p>
                <ol class="mt-1 space-y-0.5 text-sm text-text-main">
                  @for (s of e.stops; track s.position) {
                    <li>
                      <span class="text-text-secondary">{{ s.position + 1 }}.</span>
                      {{ s.hostInstitution }} · {{ s.venueCity }} ·
                      {{ s.openingDate | date:'mediumDate' }} → {{ s.closingDate | date:'mediumDate' }}
                    </li>
                  }
                </ol>
              </div>
            }
            @if (e.description) {
              <p class="text-sm text-text-main mt-3 max-w-3xl whitespace-pre-line">{{ e.description }}</p>
            }
          </div>
          <div class="flex gap-2">
            <button type="button" (click)="startEdit()"
                    class="inline-flex items-center gap-2 rounded-lg h-10 px-4 border border-border text-sm font-medium hover:bg-gray-soft">
              <span class="material-symbols-outlined" style="font-size:18px;">edit</span> Editar
            </button>
            <a routerLink="/catalog"
               class="inline-flex items-center gap-2 rounded-lg h-10 px-4 bg-primary hover:bg-primary-hover text-white text-sm font-bold">
              <span class="material-symbols-outlined" style="font-size:18px;">add</span> Buscar obras
            </a>
          </div>
        </header>
      } @else {
        <form [formGroup]="form" (ngSubmit)="save()" class="stitch-card p-6 mb-8 max-w-3xl">
          <app-exhibition-form-fields [form]="form" />
          @if (errorMessage()) {
            <p class="mt-4 text-sm text-red-700 bg-red-50 border border-red-200 rounded-lg p-3">{{ errorMessage() }}</p>
          }
          <div class="flex justify-end gap-2 mt-6">
            <button type="button" (click)="editing.set(false)"
                    class="px-4 py-2 rounded-lg bg-white border border-border hover:bg-gray-soft text-sm font-medium">Cancelar</button>
            <button type="submit" [disabled]="form.invalid || saving()"
                    class="px-4 py-2 rounded-lg bg-primary hover:bg-primary-hover text-white text-sm font-bold disabled:opacity-50">Guardar</button>
          </div>
        </form>
      }

      <section class="stitch-card overflow-hidden">
        <div class="p-5 border-b border-border">
          <h2 class="text-lg font-bold text-text-main">Obras solicitadas</h2>
        </div>
        @if ((e.loans ?? []).length === 0) {
          <p class="p-6 text-sm text-text-secondary">
            Aún no has pedido obras para esta exposición. Búscalas en el catálogo y, al solicitarlas, elige esta exposición.
          </p>
        } @else {
          <ul class="divide-y divide-border">
            @for (l of e.loans ?? []; track l.loanId) {
              <li>
                <a [routerLink]="['/app/loans', l.loanId]"
                   class="flex items-center justify-between gap-4 p-4 hover:bg-gray-soft transition-colors">
                  <div class="min-w-0">
                    <p class="text-sm font-bold text-text-main truncate">{{ l.artworkTitle }}</p>
                    <p class="text-xs text-text-secondary truncate">
                      {{ l.artworkArtist }} · {{ l.collectorName }} · {{ l.startDate | date:'shortDate' }} → {{ l.endDate | date:'shortDate' }}
                    </p>
                  </div>
                  <app-status-pill [status]="l.status" />
                </a>
              </li>
            }
          </ul>
        }
      </section>

      <!-- Compatibilidad de toda la exposición con nuestras instalaciones -->
      @if (compat(); as c) {
        @if (c.artworks.length > 0) {
          <section class="stitch-card p-6 mt-6">
            <div class="flex items-start justify-between gap-3 flex-wrap">
              <div>
                <h2 class="text-lg font-bold text-text-main">Compatibilidad de la exposición</h2>
                <p class="text-sm text-text-secondary">
                  Todas las obras frente a tus instalaciones, y entre sí si comparten sala.
                  @if (c.facilityReportUpdatedAt) { Informe de instalaciones del {{ c.facilityReportUpdatedAt | date:'mediumDate' }}. }
                </p>
              </div>
              <span class="px-2.5 py-1 rounded-full text-xs font-bold"
                    [class.bg-emerald-100]="c.result === 'OK'" [class.text-emerald-800]="c.result === 'OK'"
                    [class.bg-amber-100]="c.result === 'WARNINGS'" [class.text-amber-800]="c.result === 'WARNINGS'"
                    [class.bg-red-100]="c.result === 'BLOCKERS'" [class.text-red-800]="c.result === 'BLOCKERS'">
                {{ c.result === 'OK' ? 'Todo compatible' : c.result === 'WARNINGS' ? 'Revisar avisos' : 'Hay incompatibilidades' }}
              </span>
            </div>

            <div class="mt-4 grid grid-cols-2 sm:grid-cols-4 gap-3 text-sm">
              <div class="rounded-lg bg-gray-soft p-3">
                <p class="text-xs text-text-secondary">Temperatura común</p>
                <p class="font-bold text-text-main">{{ rangeText(c.tempMin, c.tempMax, '°C') }}</p>
              </div>
              <div class="rounded-lg bg-gray-soft p-3">
                <p class="text-xs text-text-secondary">Humedad común</p>
                <p class="font-bold text-text-main">{{ rangeText(c.rhMin, c.rhMax, '%') }}</p>
              </div>
              <div class="rounded-lg bg-gray-soft p-3">
                <p class="text-xs text-text-secondary">Luz máxima</p>
                <p class="font-bold text-text-main">{{ c.maxLux != null ? c.maxLux + ' lux' : 'Sin límite' }}</p>
              </div>
              <div class="rounded-lg bg-gray-soft p-3">
                <p class="text-xs text-text-secondary">Correos presenciales</p>
                <p class="font-bold text-text-main">{{ c.courierCount }}</p>
              </div>
            </div>

            @if (c.sharedIssues.length > 0) {
              <ul class="mt-4 space-y-2">
                @for (i of c.sharedIssues; track $index) {
                  <li class="flex items-start gap-2 text-sm"
                      [class.text-red-800]="i.severity === 'BLOCKER'" [class.text-amber-800]="i.severity === 'WARNING'">
                    <span class="material-symbols-outlined" style="font-size:18px;">{{ i.severity === 'BLOCKER' ? 'block' : 'warning' }}</span>
                    {{ i.message }}
                  </li>
                }
              </ul>
            }

            <div class="mt-5 space-y-2">
              @for (a of c.artworks; track a.loanId) {
                <details class="rounded-lg border border-border">
                  <summary class="flex items-center justify-between gap-3 p-3 cursor-pointer text-sm">
                    <span class="font-bold text-text-main truncate">{{ a.artworkTitle }}</span>
                    <span class="shrink-0 text-xs font-bold"
                          [class.text-emerald-700]="a.report.result === 'OK'"
                          [class.text-amber-700]="a.report.result === 'WARNINGS'"
                          [class.text-red-700]="a.report.result === 'BLOCKERS'">
                      {{ a.report.result === 'OK' ? 'Compatible' : a.report.result === 'WARNINGS' ? a.report.issues.length + ' aviso(s)' : 'Incompatible' }}
                    </span>
                  </summary>
                  <div class="px-3 pb-3">
                    <app-compatibility-panel [report]="a.report" />
                  </div>
                </details>
              }
            </div>
          </section>
        }
      }
    } @else {
      <div class="stitch-card p-12 text-center text-text-main font-bold">{{ errorMessage() ?? 'Exposición no encontrada.' }}</div>
    }
  `,
})
export class ExhibitionDetailComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly service = inject(ExhibitionService);

  /** Vinculado a `:id` por withComponentInputBinding(). */
  @Input() id?: string;

  protected readonly exhibition = signal<ExhibitionResponse | null>(null);
  protected readonly loading = signal(true);
  protected readonly editing = signal(false);
  protected readonly saving = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly form = buildExhibitionForm(this.fb);
  protected readonly compat = signal<ExhibitionCompatibility | null>(null);

  ngOnInit(): void {
    const id = Number(this.id);
    if (!id) {
      this.loading.set(false);
      return;
    }
    this.service.getById(id).subscribe({
      next: (e) => {
        this.exhibition.set(e);
        this.loadCompatibility(e.id);
        this.loading.set(false);
      },
      error: (err) => {
        this.errorMessage.set(err?.status === 403 ? 'No tienes acceso a esta exposición.' : null);
        this.loading.set(false);
      },
    });
  }

  startEdit(): void {
    const e = this.exhibition();
    if (!e) return;
    patchExhibitionForm(this.form, e, this.fb);
    this.errorMessage.set(null);
    this.editing.set(true);
  }

  save(): void {
    const e = this.exhibition();
    if (!e || this.form.invalid) return;
    this.saving.set(true);
    this.service.update(e.id, exhibitionFormValue(this.form)).subscribe({
      next: (updated) => {
        this.exhibition.set(updated);
        this.loadCompatibility(updated.id);
        this.saving.set(false);
        this.editing.set(false);
      },
      error: (err) => {
        this.saving.set(false);
        this.errorMessage.set(err?.error?.message ?? 'No se pudo guardar.');
      },
    });
  }

  private loadCompatibility(id: number): void {
    this.service.getCompatibility(id).subscribe({
      next: (c) => this.compat.set(c),
      error: () => this.compat.set(null),
    });
  }

  rangeText(min: number | null | undefined, max: number | null | undefined, unit: string): string {
    if (min == null && max == null) return 'Sin requisitos';
    if (min != null && max != null) return `${min}–${max} ${unit}`;
    return min != null ? `≥ ${min} ${unit}` : `≤ ${max} ${unit}`;
  }
}
