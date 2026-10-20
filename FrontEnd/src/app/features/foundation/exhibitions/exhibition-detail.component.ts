import { CommonModule } from '@angular/common';
import { Component, inject, Input, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { ExhibitionService } from '../../../core/services/exhibition.service';
import { ExhibitionResponse } from '../../../core/models/exhibition.models';
import { StatusPillComponent } from '../../../shared/components/status-pill/status-pill.component';
import { buildExhibitionForm, exhibitionFormValue, patchExhibitionForm } from './exhibition-form';
import { ExhibitionFormFieldsComponent } from './exhibition-form-fields.component';

/** Ficha de una exposición: datos editables y estado de cada obra solicitada. */
@Component({
  selector: 'app-exhibition-detail',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, StatusPillComponent, ExhibitionFormFieldsComponent],
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

  ngOnInit(): void {
    const id = Number(this.id);
    if (!id) {
      this.loading.set(false);
      return;
    }
    this.service.getById(id).subscribe({
      next: (e) => {
        this.exhibition.set(e);
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
    patchExhibitionForm(this.form, e);
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
        this.saving.set(false);
        this.editing.set(false);
      },
      error: (err) => {
        this.saving.set(false);
        this.errorMessage.set(err?.error?.message ?? 'No se pudo guardar.');
      },
    });
  }
}
