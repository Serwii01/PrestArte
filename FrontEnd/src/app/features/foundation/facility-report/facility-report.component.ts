import { CommonModule } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { FacilityReportService } from '../../../core/services/facility-report.service';
import { FacilityReportResponse } from '../../../core/models/facility.models';
import { SecureFilePipe } from '../../../shared/pipes/secure-file.pipe';

/**
 * Informe de instalaciones de la fundación: se rellena una vez y PrestArte
 * lo compara automáticamente con los requisitos de cada obra que se pide.
 */
@Component({
  selector: 'app-facility-report',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, SecureFilePipe],
  templateUrl: './facility-report.component.html',
})
export class FacilityReportComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly service = inject(FacilityReportService);

  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected readonly uploading = signal(false);
  protected readonly report = signal<FacilityReportResponse | null>(null);
  protected readonly message = signal<string | null>(null);
  protected readonly errorMessage = signal<string | null>(null);

  protected readonly form = this.fb.group({
    climateControlled: [false],
    tempMin: [null as number | null, [Validators.min(-10), Validators.max(40)]],
    tempMax: [null as number | null, [Validators.min(-10), Validators.max(40)]],
    rhMin: [null as number | null, [Validators.min(0), Validators.max(100)]],
    rhMax: [null as number | null, [Validators.min(0), Validators.max(100)]],
    minAchievableLux: [null as number | null, [Validators.min(0)]],
    uvFiltered: [false],
    guards24h: [false],
    cctv: [false],
    intrusionAlarm: [false],
    fireDetection: [false],
    fireSuppression: ['', [Validators.maxLength(200)]],
    displayCasesAvailable: [false],
    accessWidthCm: [null as number | null, [Validators.min(1)]],
    accessHeightCm: [null as number | null, [Validators.min(1)]],
    loadingDock: [false],
    notes: ['', [Validators.maxLength(5000)]],
  });

  /** Casillas de seguridad e instalaciones, para pintarlas en bucle. */
  protected readonly securityFlags: { key: string; label: string; icon: string }[] = [
    { key: 'guards24h', label: 'Vigilancia presencial 24 h', icon: 'person_alert' },
    { key: 'cctv', label: 'Videovigilancia (CCTV) grabada', icon: 'videocam' },
    { key: 'intrusionAlarm', label: 'Alarma de intrusión conectada a central', icon: 'notifications_active' },
    { key: 'fireDetection', label: 'Detección automática de incendios', icon: 'local_fire_department' },
    { key: 'displayCasesAvailable', label: 'Vitrinas disponibles', icon: 'view_in_ar' },
  ];

  ngOnInit(): void {
    this.service.getMine().subscribe({
      next: (r) => {
        this.report.set(r);
        if (r) this.form.patchValue(r);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('No se pudo cargar el informe.');
        this.loading.set(false);
      },
    });
  }

  save(): void {
    if (this.form.invalid) return;
    this.saving.set(true);
    this.message.set(null);
    this.errorMessage.set(null);
    const v = this.form.getRawValue();
    this.service
      .saveMine({
        ...v,
        fireSuppression: v.fireSuppression?.trim() || null,
        notes: v.notes?.trim() || null,
      })
      .subscribe({
        next: (r) => {
          this.report.set(r);
          this.saving.set(false);
          this.message.set('Informe guardado. Se usará en todas tus solicitudes.');
        },
        error: (err) => {
          this.saving.set(false);
          this.errorMessage.set(err?.error?.message ?? 'No se pudo guardar el informe.');
        },
      });
  }

  onDocumentSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (!file) return;
    this.uploading.set(true);
    this.errorMessage.set(null);
    this.service.uploadDocument(file).subscribe({
      next: (r) => {
        this.report.set(r);
        this.uploading.set(false);
        this.message.set('PDF adjuntado.');
      },
      error: (err) => {
        this.uploading.set(false);
        this.errorMessage.set(err?.error?.message ?? 'No se pudo subir el PDF.');
      },
    });
  }
}
