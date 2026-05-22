import { CommonModule } from '@angular/common';
import { Component, computed, inject, Input, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { ArtworkService } from '../../../core/services/artwork.service';
import { AuthService } from '../../../core/services/auth.service';
import { LoanService } from '../../../core/services/loan.service';
import { ArtworkResponse, isArtworkImage } from '../../../core/models/artwork.models';
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

  /** Trigger reactivo: cualquier cambio en el formulario refresca los computeds. */
  private readonly formTick = signal(0);

  /** Atajos de duración: añaden N días a la fecha de inicio (o a hoy). */
  protected readonly durationPresets: Array<{ label: string; days: number }> = [
    { label: '1 semana', days: 7 },
    { label: '2 semanas', days: 14 },
    { label: '1 mes', days: 30 },
    { label: '3 meses', days: 90 },
    { label: '6 meses', days: 180 },
  ];

  /** Cabeceras de días de la semana (lunes primero, estilo español). */
  protected readonly weekdayLabels = ['L', 'M', 'X', 'J', 'V', 'S', 'D'];

  /** Mes visible en el calendario (0-indexed). */
  protected readonly viewMonth = signal(new Date().getMonth());
  protected readonly viewYear = signal(new Date().getFullYear());

  /** "mayo de 2026" → se usa en la cabecera del calendario. */
  protected readonly monthLabel = computed(() => {
    const d = new Date(this.viewYear(), this.viewMonth(), 1);
    return d.toLocaleDateString('es-ES', { month: 'long', year: 'numeric' });
  });

  /**
   * Matriz 6 × 7 (42 celdas) de días para pintar el calendario, empezando en
   * lunes. Cada celda incluye las flags que el template usa para colorear
   * (inicio, fin, dentro del rango, hoy, pasado, fuera del mes).
   */
  protected readonly calendarDays = computed(() => {
    this.formTick();
    const y = this.viewYear();
    const m = this.viewMonth();
    const firstOfMonth = new Date(y, m, 1);
    // getDay devuelve 0 (domingo) – 6 (sábado). Queremos lunes=0.
    const offset = (firstOfMonth.getDay() + 6) % 7;
    const gridStart = new Date(y, m, 1 - offset);

    const today = new Date();
    today.setHours(0, 0, 0, 0);

    const startStr = this.form.controls.startDate.value;
    const endStr = this.form.controls.endDate.value;
    const start = startStr ? new Date(startStr) : null;
    const end = endStr ? new Date(endStr) : null;
    if (start) start.setHours(0, 0, 0, 0);
    if (end) end.setHours(0, 0, 0, 0);

    const cells: Array<{
      iso: string;
      day: number;
      inMonth: boolean;
      isPast: boolean;
      isToday: boolean;
      isStart: boolean;
      isEnd: boolean;
      inRange: boolean;
    }> = [];

    for (let i = 0; i < 42; i++) {
      const d = new Date(gridStart);
      d.setDate(gridStart.getDate() + i);
      d.setHours(0, 0, 0, 0);
      const iso = this.toIso(d);
      const time = d.getTime();
      const isStart = !!start && time === start.getTime();
      const isEnd = !!end && time === end.getTime();
      const inRange =
        !!start && !!end && time > start.getTime() && time < end.getTime();
      cells.push({
        iso,
        day: d.getDate(),
        inMonth: d.getMonth() === m,
        isPast: time < today.getTime(),
        isToday: time === today.getTime(),
        isStart,
        isEnd,
        inRange,
      });
    }
    return cells;
  });

  /** Avanza/retrocede el mes visible. */
  prevMonth(): void {
    let m = this.viewMonth() - 1;
    let y = this.viewYear();
    if (m < 0) {
      m = 11;
      y--;
    }
    this.viewMonth.set(m);
    this.viewYear.set(y);
  }

  nextMonth(): void {
    let m = this.viewMonth() + 1;
    let y = this.viewYear();
    if (m > 11) {
      m = 0;
      y++;
    }
    this.viewMonth.set(m);
    this.viewYear.set(y);
  }

  /**
   * Lógica de selección de rango: el primer click marca el inicio; el segundo,
   * el fin (si es posterior). Click en el día inicio limpia la selección.
   */
  pickDay(iso: string, disabled: boolean): void {
    if (disabled) return;
    const start = this.form.controls.startDate.value;
    const end = this.form.controls.endDate.value;

    if (!start || (start && end)) {
      // Empezamos selección nueva.
      this.form.patchValue({ startDate: iso, endDate: '' });
      return;
    }
    // Solo hay startDate: este click define el endDate.
    if (iso < start) {
      // Click anterior al inicio → reiniciamos.
      this.form.patchValue({ startDate: iso, endDate: '' });
    } else if (iso === start) {
      // Click sobre el inicio → limpiar.
      this.form.patchValue({ startDate: '', endDate: '' });
    } else {
      this.form.patchValue({ endDate: iso });
    }
  }

  /** Saltar a hoy y marcarlo como inicio. */
  goToday(): void {
    const now = new Date();
    this.viewMonth.set(now.getMonth());
    this.viewYear.set(now.getFullYear());
    this.pickDay(this.toIso(now), false);
  }

  /** Vaciar la selección. */
  clearDates(): void {
    this.form.patchValue({ startDate: '', endDate: '' });
  }

  /** Días entre fecha inicio y fin (incluyendo ambos extremos como "1 día"). */
  protected readonly durationDays = computed(() => {
    this.formTick();
    const start = this.form.controls.startDate.value;
    const end = this.form.controls.endDate.value;
    if (!start || !end) return null;
    const s = new Date(start);
    const e = new Date(end);
    if (isNaN(s.getTime()) || isNaN(e.getTime())) return null;
    const diff = (e.getTime() - s.getTime()) / (1000 * 60 * 60 * 24);
    if (diff < 0) return null;
    return Math.round(diff) + 1; // +1: ambos extremos cuentan
  });

  /** "12 de mayo de 2026" (formato legible bajo cada input). */
  protected readonly startLabel = computed(() => {
    this.formTick();
    return this.prettyDate(this.form.controls.startDate.value);
  });
  protected readonly endLabel = computed(() => {
    this.formTick();
    return this.prettyDate(this.form.controls.endDate.value);
  });

  /** Para mostrar la imagen principal en la cabecera. */
  protected readonly heroImageUrl = computed(() => {
    const a = this.artwork();
    if (!a?.files || a.files.length === 0) return null;
    const first = a.files.find(isArtworkImage);
    return first ? this.artworkService.fileUrl(first.id) : null;
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

    // Refrescamos los computeds (duración, labels) cada vez que cambia el form.
    this.form.valueChanges.subscribe(() => this.formTick.update((n) => n + 1));
  }

  /** "12 de mayo de 2026" o '' si la fecha no es válida. */
  private prettyDate(iso?: string): string {
    if (!iso) return '';
    const d = new Date(iso);
    if (isNaN(d.getTime())) return '';
    return d.toLocaleDateString('es-ES', {
      weekday: 'short',
      day: 'numeric',
      month: 'long',
      year: 'numeric',
    });
  }

  /** Aplica un atajo de duración: deja startDate como esté (o hoy) y suma N días. */
  applyPreset(days: number): void {
    const start = this.form.controls.startDate.value || this.todayIso;
    const startDate = new Date(start);
    const end = new Date(startDate);
    end.setDate(startDate.getDate() + days - 1); // -1 porque el rango es inclusivo
    this.form.patchValue({
      startDate: this.toIso(startDate),
      endDate: this.toIso(end),
    });
  }

  /** Convierte un Date a yyyy-MM-dd en hora local (sin offset UTC). */
  private toIso(d: Date): string {
    const yyyy = d.getFullYear();
    const mm = String(d.getMonth() + 1).padStart(2, '0');
    const dd = String(d.getDate()).padStart(2, '0');
    return `${yyyy}-${mm}-${dd}`;
  }

  /** True si el rango actual coincide exactamente con este preset. */
  isPresetActive(days: number): boolean {
    return this.durationDays() === days;
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
