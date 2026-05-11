import { CommonModule } from '@angular/common';
import { Component, computed, inject, Input, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { ArtworkService } from '../../../core/services/artwork.service';
import { AuthService } from '../../../core/services/auth.service';
import { LoanService } from '../../../core/services/loan.service';
import { ArtworkResponse } from '../../../core/models/artwork.models';
import { CreateLoanRequest } from '../../../core/models/loan.models';

@Component({
  selector: 'app-request-loan',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './request-loan.component.html',
  styleUrl: './request-loan.component.scss',
})
export class RequestLoanComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly artworkService = inject(ArtworkService);
  private readonly loanService = inject(LoanService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  /** Vinculado a `:artworkId` por withComponentInputBinding(). */
  @Input() artworkId?: string;

  protected readonly artwork = signal<ArtworkResponse | null>(null);
  protected readonly loading = signal(true);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly fieldErrors = signal<Record<string, string>>({});
  protected readonly submitting = signal(false);

  /** Hoy en formato yyyy-MM-dd para el min de los inputs date. */
  protected readonly todayIso = new Date().toISOString().slice(0, 10);

  protected readonly form = this.fb.nonNullable.group({
    startDate: ['', [Validators.required]],
    endDate: ['', [Validators.required]],
    agreedConditions: [''],
  });

  /** Para mostrar la imagen principal en la cabecera. */
  protected readonly heroImageUrl = computed(() => {
    const a = this.artwork();
    if (!a?.files || a.files.length === 0) return null;
    return this.artworkService.fileUrl(a.files[0].id);
  });

  ngOnInit(): void {
    const id = Number(this.artworkId);
    if (!id) {
      this.errorMessage.set('Identificador de obra inválido.');
      this.loading.set(false);
      return;
    }
    this.artworkService.getById(id).subscribe({
      next: (a) => {
        this.artwork.set(a);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('No se pudo cargar la obra.');
        this.loading.set(false);
      },
    });
  }

  submit(): void {
    if (this.form.invalid) return;
    const a = this.artwork();
    const foundationId = this.auth.userId();
    if (!a || foundationId == null) return;

    this.submitting.set(true);
    this.errorMessage.set(null);
    this.fieldErrors.set({});

    const v = this.form.getRawValue();

    // Validación cliente: endDate > startDate.
    if (v.startDate >= v.endDate) {
      this.submitting.set(false);
      this.errorMessage.set('La fecha fin debe ser posterior a la fecha de inicio.');
      return;
    }

    const body: CreateLoanRequest = {
      artworkId: a.id,
      foundationId,
      startDate: v.startDate,
      endDate: v.endDate,
      agreedConditions: v.agreedConditions || undefined,
    };

    this.loanService.create(body).subscribe({
      next: () => {
        this.submitting.set(false);
        this.router.navigate(['/app/foundation']);
      },
      error: (err) => {
        this.submitting.set(false);
        const apiErrors = err?.error?.fieldErrors;
        if (apiErrors && typeof apiErrors === 'object') {
          this.fieldErrors.set(apiErrors);
        }
        this.errorMessage.set(err?.error?.message ?? 'No se pudo crear la solicitud.');
      },
    });
  }

  fieldErr(name: string): string | null {
    return this.fieldErrors()[name] ?? null;
  }
}
