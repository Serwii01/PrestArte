import { CommonModule } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { ExhibitionService } from '../../../core/services/exhibition.service';
import { ExhibitionResponse } from '../../../core/models/exhibition.models';
import { buildExhibitionForm, exhibitionFormValue } from './exhibition-form';
import { ExhibitionFormFieldsComponent } from './exhibition-form-fields.component';

/** Exposiciones de la fundación: listado y alta. */
@Component({
  selector: 'app-exhibitions',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, ExhibitionFormFieldsComponent],
  template: `
    <header class="mb-6 flex items-start justify-between gap-4 flex-wrap">
      <div>
        <h1 class="text-3xl md:text-4xl font-black text-text-main tracking-tight">Exposiciones</h1>
        <p class="text-text-secondary mt-2 max-w-2xl">
          Agrupa tus solicitudes por exposición. Los coleccionistas verán qué se expone, dónde y cuándo,
          y tú seguirás el estado de todas las obras desde un único sitio.
        </p>
      </div>
      @if (!showForm()) {
        <button type="button" (click)="showForm.set(true)"
                class="inline-flex items-center gap-2 rounded-lg h-11 px-5 bg-primary hover:bg-primary-hover text-white text-sm font-bold">
          <span class="material-symbols-outlined" style="font-size:18px;">add</span>
          Nueva exposición
        </button>
      }
    </header>

    @if (showForm()) {
      <form [formGroup]="form" (ngSubmit)="create()" class="stitch-card p-6 mb-8 max-w-3xl">
        <h2 class="text-lg font-bold text-text-main mb-4">Nueva exposición</h2>
        <app-exhibition-form-fields [form]="form" />
        @if (errorMessage()) {
          <p class="mt-4 text-sm text-red-700 bg-red-50 border border-red-200 rounded-lg p-3">{{ errorMessage() }}</p>
        }
        <div class="flex justify-end gap-2 mt-6">
          <button type="button" (click)="showForm.set(false)"
                  class="px-4 py-2 rounded-lg bg-white border border-border hover:bg-gray-soft text-sm font-medium">Cancelar</button>
          <button type="submit" [disabled]="form.invalid || saving()"
                  class="px-4 py-2 rounded-lg bg-primary hover:bg-primary-hover text-white text-sm font-bold disabled:opacity-50">
            Crear exposición
          </button>
        </div>
      </form>
    }

    @if (loading()) {
      <div class="stitch-card p-12 text-center text-text-secondary">Cargando…</div>
    } @else if (exhibitions().length === 0) {
      <div class="stitch-card p-12 text-center">
        <span class="material-symbols-outlined text-text-secondary" style="font-size:40px;">museum</span>
        <p class="text-text-main font-bold mt-2">Todavía no tienes exposiciones</p>
        <p class="text-sm text-text-secondary">Crea una para empezar a solicitar obras.</p>
      </div>
    } @else {
      <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
        @for (e of exhibitions(); track e.id) {
          <a [routerLink]="['/app/foundation/exhibitions', e.id]"
             class="stitch-card p-5 hover:border-primary transition-colors block">
            <p class="text-lg font-bold text-text-main">{{ e.title }}</p>
            <p class="text-sm text-text-secondary">{{ e.venueName }} · {{ e.venueCity }}</p>
            <p class="text-sm text-text-secondary mt-1">
              {{ e.openingDate | date:'mediumDate' }} → {{ e.closingDate | date:'mediumDate' }}
            </p>
            <div class="mt-3 flex flex-wrap gap-2 text-xs">
              <span class="px-2 py-0.5 rounded-full bg-gray-soft text-text-main font-bold">
                {{ e.loans?.length ?? 0 }} obras solicitadas
              </span>
              @if (accepted(e) > 0) {
                <span class="px-2 py-0.5 rounded-full bg-emerald-100 text-emerald-800 font-bold">{{ accepted(e) }} aceptadas</span>
              }
              @if (pending(e) > 0) {
                <span class="px-2 py-0.5 rounded-full bg-amber-100 text-amber-800 font-bold">{{ pending(e) }} pendientes</span>
              }
            </div>
          </a>
        }
      </div>
    }
  `,
})
export class ExhibitionsComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly service = inject(ExhibitionService);
  private readonly router = inject(Router);

  protected readonly exhibitions = signal<ExhibitionResponse[]>([]);
  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected readonly showForm = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly form = buildExhibitionForm(this.fb);

  ngOnInit(): void {
    this.service.getMine().subscribe({
      next: (list) => {
        this.exhibitions.set(list);
        this.loading.set(false);
        if (list.length === 0) this.showForm.set(true);
      },
      error: () => this.loading.set(false),
    });
  }

  create(): void {
    if (this.form.invalid) return;
    this.saving.set(true);
    this.errorMessage.set(null);
    this.service.create(exhibitionFormValue(this.form)).subscribe({
      next: (e) => this.router.navigate(['/app/foundation/exhibitions', e.id]),
      error: (err) => {
        this.saving.set(false);
        this.errorMessage.set(err?.error?.message ?? 'No se pudo crear la exposición.');
      },
    });
  }

  protected accepted(e: ExhibitionResponse): number {
    return (e.loans ?? []).filter((l) => !['REQUESTED', 'REJECTED', 'CANCELLED'].includes(l.status)).length;
  }

  protected pending(e: ExhibitionResponse): number {
    return (e.loans ?? []).filter((l) => l.status === 'REQUESTED').length;
  }
}
